package it.downfall.reversky.api.auth.service;

import static it.downfall.reversky.api.support.TestUsers.PASSWORD;
import static it.downfall.reversky.api.support.TestUsers.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.auth.dto.ForgotPasswordRequest;
import it.downfall.reversky.api.auth.dto.LoginRequest;
import it.downfall.reversky.api.auth.dto.RefreshTokenRequest;
import it.downfall.reversky.api.auth.dto.RegisterRequest;
import it.downfall.reversky.api.auth.dto.ResetPasswordRequest;
import it.downfall.reversky.api.auth.dto.TokenResponse;
import it.downfall.reversky.api.auth.dto.VerifyEmailRequest;
import it.downfall.reversky.api.auth.entity.EmailVerificationToken;
import it.downfall.reversky.api.auth.entity.PasswordResetToken;
import it.downfall.reversky.api.auth.entity.RefreshToken;
import it.downfall.reversky.api.auth.exception.AccountDisabledException;
import it.downfall.reversky.api.auth.exception.EmailAlreadyUsedException;
import it.downfall.reversky.api.auth.exception.EmailAlreadyVerifiedException;
import it.downfall.reversky.api.auth.exception.InvalidCredentialsException;
import it.downfall.reversky.api.auth.exception.InvalidEmailVerificationTokenException;
import it.downfall.reversky.api.auth.exception.InvalidPasswordResetTokenException;
import it.downfall.reversky.api.auth.exception.InvalidRefreshTokenException;
import it.downfall.reversky.api.auth.exception.UsernameAlreadyUsedException;
import it.downfall.reversky.api.auth.repository.EmailVerificationTokenRepository;
import it.downfall.reversky.api.auth.repository.PasswordResetTokenRepository;
import it.downfall.reversky.api.auth.repository.RefreshTokenRepository;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.TokenHasher;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.exception.UserNotFoundException;
import it.downfall.reversky.api.user.service.UserService;

/**
 * Tutti i flussi di {@link AuthService} su un PostgreSQL vero (docs/autenticazione).
 * <p>
 * La classe non è {@code @Transactional}: ogni chiamata deve fare il commit come in produzione,
 * altrimenti il test di concorrenza non avrebbe senso. Ogni test crea un proprio utente, quindi
 * i test non si influenzano anche se condividono il database. {@link EmailSender} è un mock:
 * nessuna email parte davvero, e da lì si legge il token in chiaro che l'utente riceverebbe.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class AuthServiceIT {

	private static final String NEW_PASSWORD = "BrandNewPassword2";

	@Autowired
	private AuthService authService;

	@Autowired
	private UserService userService;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private TokenHasher tokenHasher;

	@Autowired
	private PasswordResetTokenRepository passwordResetTokenRepository;

	@Autowired
	private EmailVerificationTokenRepository emailVerificationTokenRepository;

	@Autowired
	private TestUsers testUsers;

	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("Correct credentials return a Bearer token pair")
	void login_correctCredentials_returnsBearerTokens() {
		// Arrange
		User user = testUsers.create();

		// Act
		TokenResponse response = login(user);

		// Assert
		assertThat(response.accessToken()).isNotBlank();
		assertThat(response.refreshToken()).isNotBlank();
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isEqualTo(900);
	}

	@Test
	@DisplayName("Login stores the hash of the refresh token, never the raw value")
	void login_correctCredentials_storesOnlyTokenHash() {
		// Arrange
		User user = testUsers.create();

		// Act
		String rawToken = login(user).refreshToken();

		// Assert
		assertThat(refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawToken))).isPresent();
		assertThat(refreshTokenRepository.findByTokenHash(rawToken)).isEmpty();
	}

	@Test
	@DisplayName("The email is matched regardless of case")
	void login_emailInUpperCase_returnsTokens() {
		// Arrange
		User user = testUsers.create();
		LoginRequest request = new LoginRequest(user.getEmail().toUpperCase(Locale.ROOT), PASSWORD);

		// Act
		TokenResponse response = authService.login(request);

		// Assert
		assertThat(response.refreshToken()).isNotBlank();
	}

	@Test
	@DisplayName("A wrong password is rejected")
	void login_wrongPassword_throwsInvalidCredentials() {
		// Arrange
		User user = testUsers.create();
		LoginRequest request = new LoginRequest(user.getEmail(), "WrongPassword1");

		// Act + Assert
		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	@DisplayName("An unknown email gets the same error as a wrong password")
	void login_unknownEmail_throwsInvalidCredentials() {
		// Arrange
		LoginRequest request = new LoginRequest("nobody-" + uniqueSuffix() + "@example.com", PASSWORD);

		// Act + Assert
		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	@DisplayName("A disabled account cannot log in")
	void login_disabledAccount_throwsAccountDisabled() {
		// Arrange
		User user = testUsers.create();
		testUsers.disable(user);

		// Act + Assert
		assertThatThrownBy(() -> login(user))
			.isInstanceOf(AccountDisabledException.class);
	}

	@Test
	@DisplayName("A disabled account is only revealed to someone who knows the password")
	void login_disabledAccountAndWrongPassword_throwsInvalidCredentials() {
		// Arrange
		User user = testUsers.create();
		testUsers.disable(user);
		LoginRequest request = new LoginRequest(user.getEmail(), "WrongPassword1");

		// Act + Assert
		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	@DisplayName("A valid refresh token returns a new token pair")
	void refresh_validToken_returnsNewTokens() {
		// Arrange
		String oldToken = login(testUsers.create()).refreshToken();

		// Act
		TokenResponse response = refresh(oldToken);

		// Assert
		assertThat(response.accessToken()).isNotBlank();
		assertThat(response.refreshToken()).isNotBlank().isNotEqualTo(oldToken);
	}

	@Test
	@DisplayName("Rotation: a refresh token cannot be used twice")
	void refresh_alreadyUsedToken_throwsInvalidRefreshToken() {
		// Arrange
		String oldToken = login(testUsers.create()).refreshToken();
		refresh(oldToken);

		// Act + Assert
		assertThatThrownBy(() -> refresh(oldToken))
			.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	@DisplayName("Rotation: the new refresh token works")
	void refresh_tokenFromPreviousRefresh_returnsNewTokens() {
		// Arrange
		String secondToken = refresh(login(testUsers.create()).refreshToken()).refreshToken();

		// Act
		TokenResponse response = refresh(secondToken);

		// Assert
		assertThat(response.refreshToken()).isNotBlank().isNotEqualTo(secondToken);
	}

	@Test
	@DisplayName("An unknown refresh token is rejected")
	void refresh_unknownToken_throwsInvalidRefreshToken() {
		// Arrange
		String unknownToken = tokenHasher.generateToken();

		// Act + Assert
		assertThatThrownBy(() -> refresh(unknownToken))
			.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	@DisplayName("An expired refresh token is rejected")
	void refresh_expiredToken_throwsInvalidRefreshToken() {
		// Arrange
		String rawToken = tokenHasher.generateToken();
		Instant expiredAt = Instant.now().minusSeconds(60);
		refreshTokenRepository.save(new RefreshToken(testUsers.create(), tokenHasher.hash(rawToken), expiredAt));

		// Act + Assert
		assertThatThrownBy(() -> refresh(rawToken))
			.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	@DisplayName("A disabled account cannot refresh its session")
	void refresh_disabledAccount_throwsAccountDisabled() {
		// Arrange
		User user = testUsers.create();
		String rawToken = login(user).refreshToken();
		testUsers.disable(user);

		// Act + Assert
		assertThatThrownBy(() -> refresh(rawToken))
			.isInstanceOf(AccountDisabledException.class);
	}

	@Test
	@DisplayName("Two simultaneous refreshes with the same token: only one succeeds")
	void refresh_sameTokenConcurrently_onlyOneSucceeds() throws Exception {
		// Arrange
		String rawToken = login(testUsers.create()).refreshToken();
		CountDownLatch start = new CountDownLatch(1);

		// false = 401 pulito; qualsiasi altra eccezione esce da get() e fa fallire il test
		Callable<Boolean> attempt = () -> {
			start.await();
			try {
				refresh(rawToken);
				return true;
			} catch (InvalidRefreshTokenException e) {
				return false;
			}
		};

		// Act
		boolean first;
		boolean second;
		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			Future<Boolean> firstResult = executor.submit(attempt);
			Future<Boolean> secondResult = executor.submit(attempt);
			start.countDown();
			first = firstResult.get();
			second = secondResult.get();
		}

		// Assert
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	@DisplayName("After logout the refresh token no longer works")
	void logout_validToken_preventsLaterRefresh() {
		// Arrange
		String rawToken = login(testUsers.create()).refreshToken();

		// Act
		authService.logout(new RefreshTokenRequest(rawToken));

		// Assert
		assertThatThrownBy(() -> refresh(rawToken))
			.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	@DisplayName("Logout only ends the session of that token")
	void logout_oneOfTwoSessions_keepsTheOtherSession() {
		// Arrange
		User user = testUsers.create();
		String firstSession = login(user).refreshToken();
		String secondSession = login(user).refreshToken();

		// Act
		authService.logout(new RefreshTokenRequest(firstSession));

		// Assert
		assertThat(refresh(secondSession).refreshToken()).isNotBlank();
	}

	@Test
	@DisplayName("Logout with an unknown token is not an error")
	void logout_unknownToken_doesNotThrow() {
		// Arrange
		RefreshTokenRequest request = new RefreshTokenRequest(tokenHasher.generateToken());

		// Act + Assert
		assertThatCode(() -> authService.logout(request)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("A registered email receives a reset link, and only its hash is stored")
	void forgotPassword_registeredEmail_sendsLinkAndStoresHash() {
		// Arrange
		User user = testUsers.create();

		// Act
		String rawToken = requestResetToken(user);

		// Assert
		assertThat(passwordResetTokenRepository.findByTokenHash(tokenHasher.hash(rawToken))).isPresent();
		assertThat(passwordResetTokenRepository.findByTokenHash(rawToken)).isEmpty();
	}

	@Test
	@DisplayName("An unknown email gets no error and no email")
	void forgotPassword_unknownEmail_sendsNothing() {
		// Arrange
		ForgotPasswordRequest request = new ForgotPasswordRequest("nobody-" + uniqueSuffix() + "@example.com");

		// Act
		authService.forgotPassword(request);

		// Assert
		verify(emailSender, never()).sendPasswordReset(any(), any());
	}

	@Test
	@DisplayName("A new reset request invalidates the previous link")
	void forgotPassword_secondRequest_invalidatesPreviousToken() {
		// Arrange
		User user = testUsers.create();
		String firstToken = requestResetToken(user);
		requestResetToken(user);

		// Act + Assert
		assertThatThrownBy(() -> resetPassword(firstToken))
			.isInstanceOf(InvalidPasswordResetTokenException.class);
	}

	@Test
	@DisplayName("After a reset the new password works")
	void resetPassword_validToken_newPasswordWorks() {
		// Arrange
		User user = testUsers.create();
		String rawToken = requestResetToken(user);

		// Act
		resetPassword(rawToken);

		// Assert
		assertThat(authService.login(new LoginRequest(user.getEmail(), NEW_PASSWORD)).refreshToken()).isNotBlank();
	}

	@Test
	@DisplayName("After a reset the old password no longer works")
	void resetPassword_validToken_oldPasswordRejected() {
		// Arrange
		User user = testUsers.create();
		String rawToken = requestResetToken(user);

		// Act
		resetPassword(rawToken);

		// Assert
		assertThatThrownBy(() -> login(user))
			.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	@DisplayName("A reset ends every session of the user")
	void resetPassword_validToken_revokesAllRefreshTokens() {
		// Arrange
		User user = testUsers.create();
		String firstSession = login(user).refreshToken();
		String secondSession = login(user).refreshToken();
		String rawToken = requestResetToken(user);

		// Act
		resetPassword(rawToken);

		// Assert
		assertThatThrownBy(() -> refresh(firstSession))
			.isInstanceOf(InvalidRefreshTokenException.class);
		assertThatThrownBy(() -> refresh(secondSession))
			.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	@DisplayName("A reset link cannot be used twice")
	void resetPassword_alreadyUsedToken_throwsInvalidResetToken() {
		// Arrange
		String rawToken = requestResetToken(testUsers.create());
		resetPassword(rawToken);

		// Act + Assert
		assertThatThrownBy(() -> resetPassword(rawToken))
			.isInstanceOf(InvalidPasswordResetTokenException.class);
	}

	@Test
	@DisplayName("An unknown reset token is rejected")
	void resetPassword_unknownToken_throwsInvalidResetToken() {
		// Arrange
		String unknownToken = tokenHasher.generateToken();

		// Act + Assert
		assertThatThrownBy(() -> resetPassword(unknownToken))
			.isInstanceOf(InvalidPasswordResetTokenException.class);
	}

	@Test
	@DisplayName("An expired reset token is rejected")
	void resetPassword_expiredToken_throwsInvalidResetToken() {
		// Arrange
		String rawToken = tokenHasher.generateToken();
		Instant expiredAt = Instant.now().minusSeconds(60);
		passwordResetTokenRepository.save(new PasswordResetToken(testUsers.create(), tokenHasher.hash(rawToken), expiredAt));

		// Act + Assert
		assertThatThrownBy(() -> resetPassword(rawToken))
			.isInstanceOf(InvalidPasswordResetTokenException.class);
	}

	@Test
	@DisplayName("A new user starts unverified and receives a verification link")
	void register_newUser_sendsLinkAndStartsUnverified() {
		// Arrange
		String suffix = uniqueSuffix();
		String email = suffix + "@example.com";

		// Act
		authService.register(new RegisterRequest("user_" + suffix, email, PASSWORD, true, true));

		// Assert
		verify(emailSender).sendEmailVerification(eq(email), any());
		assertThat(userService.findByEmail(email).orElseThrow().isEmailVerified()).isFalse();
	}

	@Test
	@DisplayName("A username that is already used is rejected")
	void register_usernameAlreadyUsed_throwsUsernameAlreadyUsed() {
		// Arrange
		User user = testUsers.create();
		RegisterRequest request = new RegisterRequest(user.getUsername(), uniqueSuffix() + "@example.com", PASSWORD, true, true);

		// Act + Assert
		assertThatThrownBy(() -> authService.register(request))
			.isInstanceOf(UsernameAlreadyUsedException.class);
	}

	@Test
	@DisplayName("An email that is already used is rejected, regardless of case")
	void register_emailAlreadyUsedInUpperCase_throwsEmailAlreadyUsed() {
		// Arrange
		User user = testUsers.create();
		String otherUsername = "user_" + uniqueSuffix();
		RegisterRequest request = new RegisterRequest(otherUsername, user.getEmail().toUpperCase(Locale.ROOT), PASSWORD, true, true);

		// Act + Assert
		assertThatThrownBy(() -> authService.register(request))
			.isInstanceOf(EmailAlreadyUsedException.class);
	}

	@Test
	@DisplayName("A valid verification link confirms the email")
	void verifyEmail_validToken_marksEmailVerified() {
		// Arrange
		User user = testUsers.create();
		String rawToken = requestVerificationToken(user);

		// Act
		verifyEmail(rawToken);

		// Assert
		assertThat(userService.getById(user.getId()).isEmailVerified()).isTrue();
	}

	@Test
	@DisplayName("A verification link cannot be used twice")
	void verifyEmail_alreadyUsedToken_throwsInvalidVerificationToken() {
		// Arrange
		String rawToken = requestVerificationToken(testUsers.create());
		verifyEmail(rawToken);

		// Act + Assert
		assertThatThrownBy(() -> verifyEmail(rawToken))
			.isInstanceOf(InvalidEmailVerificationTokenException.class);
	}

	@Test
	@DisplayName("An unknown verification token is rejected")
	void verifyEmail_unknownToken_throwsInvalidVerificationToken() {
		// Arrange
		String unknownToken = tokenHasher.generateToken();

		// Act + Assert
		assertThatThrownBy(() -> verifyEmail(unknownToken))
			.isInstanceOf(InvalidEmailVerificationTokenException.class);
	}

	@Test
	@DisplayName("An expired verification token is rejected")
	void verifyEmail_expiredToken_throwsInvalidVerificationToken() {
		// Arrange
		String rawToken = tokenHasher.generateToken();
		Instant expiredAt = Instant.now().minusSeconds(60);
		emailVerificationTokenRepository.save(new EmailVerificationToken(testUsers.create(), tokenHasher.hash(rawToken), expiredAt));

		// Act + Assert
		assertThatThrownBy(() -> verifyEmail(rawToken))
			.isInstanceOf(InvalidEmailVerificationTokenException.class);
	}

	@Test
	@DisplayName("A new verification link invalidates the previous one")
	void resendEmailVerification_secondRequest_invalidatesPreviousToken() {
		// Arrange
		User user = testUsers.create();
		String firstToken = requestVerificationToken(user);
		requestVerificationToken(user);

		// Act + Assert
		assertThatThrownBy(() -> verifyEmail(firstToken))
			.isInstanceOf(InvalidEmailVerificationTokenException.class);
	}

	@Test
	@DisplayName("No new link for an email that is already verified")
	void resendEmailVerification_alreadyVerified_throwsEmailAlreadyVerified() {
		// Arrange
		User user = testUsers.create();
		verifyEmail(requestVerificationToken(user));

		// Act + Assert
		assertThatThrownBy(() -> authService.resendEmailVerification(user.getId()))
			.isInstanceOf(EmailAlreadyVerifiedException.class);
	}

	@Test
	@DisplayName("A user id that does not exist is rejected")
	void resendEmailVerification_unknownUser_throwsUserNotFound() {
		// Act + Assert
		assertThatThrownBy(() -> authService.resendEmailVerification(Integer.MAX_VALUE))
			.isInstanceOf(UserNotFoundException.class);
	}

	// Chiede un nuovo link e restituisce il token dell'ultima email inviata all'utente
	private String requestVerificationToken(User user) {
		authService.resendEmailVerification(user.getId());

		ArgumentCaptor<String> rawToken = ArgumentCaptor.forClass(String.class);
		verify(emailSender, atLeastOnce()).sendEmailVerification(eq(user.getEmail()), rawToken.capture());

		return rawToken.getValue();
	}

	private void verifyEmail(String rawToken) {
		authService.verifyEmail(new VerifyEmailRequest(rawToken));
	}

	// Chiede il reset e restituisce il token dell'ultima email inviata all'utente
	private String requestResetToken(User user) {
		authService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));

		ArgumentCaptor<String> rawToken = ArgumentCaptor.forClass(String.class);
		verify(emailSender, atLeastOnce()).sendPasswordReset(eq(user.getEmail()), rawToken.capture());

		return rawToken.getValue();
	}

	private void resetPassword(String rawToken) {
		authService.resetPassword(new ResetPasswordRequest(rawToken, NEW_PASSWORD));
	}

	private TokenResponse login(User user) {
		return authService.login(new LoginRequest(user.getEmail(), PASSWORD));
	}

	private TokenResponse refresh(String rawToken) {
		return authService.refresh(new RefreshTokenRequest(rawToken));
	}
}
