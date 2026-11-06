package it.downfall.reversky.api.auth.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.common.security.SecurityProperties;
import it.downfall.reversky.api.common.security.TokenHasher;
import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.exception.UserNotFoundException;
import it.downfall.reversky.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registrazione, login, token e recupero dell'account (docs/autenticazione).
 * <p>
 * I token salvati (refresh, reset, verifica) sono sempre hash SHA-256: il valore in chiaro
 * esiste solo nella risposta o nell'email inviata all'utente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserService userService;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final EmailVerificationTokenRepository emailVerificationTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenHasher tokenHasher;
	private final JwtService jwtService;
	private final EmailSender emailSender;
	private final SecurityProperties securityProperties;
	private final Clock clock;

	/**
	 * Registra un nuovo utente con ruolo USER.
	 * <p>
	 * Nella stessa transazione crea l'utente e il token di verifica, poi invia l'email di verifica.
	 *
	 * @throws UsernameAlreadyUsedException se lo username è già usato
	 * @throws EmailAlreadyUsedException    se l'email è già usata
	 */
	@Transactional
	public void register(RegisterRequest request) {

		if (userService.isUsernameTaken(request.username())) {
			throw new UsernameAlreadyUsedException(request.username());
		}

		if (userService.isEmailTaken(request.email())) {
			throw new EmailAlreadyUsedException();
		}

		Instant now = Instant.now(clock);
		String passwordHash = passwordEncoder.encode(request.password());

		User user = userService.createUser(request.username(), request.email(), passwordHash, now);

		// TODO(@tommasofatticcioni): creare inventories con signup_bonus e la riga SIGNUP in credit_transactions (docs/crediti)

		sendEmailVerification(user, now);
		log.info("User {} registered", user.getId());
	}

	// Crea un nuovo token di verifica (cancellando i precedenti) e invia l'email.
	private void sendEmailVerification(User user, Instant now) {
		emailVerificationTokenRepository.deleteAllByUserId(user.getId());

		String token = tokenHasher.generateToken();
		Instant expiresAt = now.plus(securityProperties.emailVerificationTokenTtl());

		emailVerificationTokenRepository.save(new EmailVerificationToken(user, tokenHasher.hash(token), expiresAt));
		emailSender.sendEmailVerification(user.getEmail(), token);
	}

	/**
	 * Verifica le credenziali e crea una nuova sessione.
	 *
	 * @return access token e refresh token
	 * @throws InvalidCredentialsException se l'email non esiste o la password è errata
	 * @throws AccountDisabledException    se la password è corretta ma l'account è disabilitato
	 */
	@Transactional
	public TokenResponse login(LoginRequest request) {

		User user = userService.findByEmail(request.email())
			.orElseThrow(() -> new InvalidCredentialsException());

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}

		if (!user.isEnabled()) {
			throw new AccountDisabledException();
		}

		return issueTokens(user, Instant.now(clock));
	}

	/**
	 * Rinnova la sessione con rotazione: il refresh token usato viene eliminato e ne viene creato uno nuovo.
	 * @return nuovo access token e nuovo refresh token
	 * @throws InvalidRefreshTokenException se il token non esiste, è scaduto o è già stato usato
	 * @throws AccountDisabledException     se l'account è stato disabilitato
	 */
	@Transactional
	public TokenResponse refresh(RefreshTokenRequest request) {

		Instant now = Instant.now(clock);
		String tokenHash = tokenHasher.hash(request.refreshToken());

		RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
			.orElseThrow(() -> new InvalidRefreshTokenException());

		if (token.isExpired(now)) {
			throw new InvalidRefreshTokenException();
		}

		User user = token.getUser();
		if (!user.isEnabled()) {
			throw new AccountDisabledException();
		}

		// Se restituisce 0 significa che il token è già stato utilizzato.
		if (refreshTokenRepository.deleteByTokenHash(tokenHash) == 0) {
			throw new InvalidRefreshTokenException();
		}

		return issueTokens(user, now);
	}

	/**
	 * Termina la sessione eliminando il refresh token. Un token sconosciuto non è un errore: risponde comunque 204.
	 */
	@Transactional
	public void logout(RefreshTokenRequest request) {
		refreshTokenRepository.deleteByTokenHash(tokenHasher.hash(request.refreshToken()));
	}

	/**
	 * Invia il link per reimpostare la password, se l'email è registrata.
	 * <p>
	 * Non lancia eccezioni se l'email non esiste: la risposta è sempre 204 (docs/autenticazione).
	 */
	@Transactional
	public void forgotPassword(ForgotPasswordRequest request) {
		userService.findByEmail(request.email()).ifPresent(user -> sendPasswordReset(user, Instant.now(clock)));
	}

	/**
	 * Imposta la nuova password e chiude tutte le sessioni dell'utente.
	 *
	 * @throws InvalidPasswordResetTokenException se il token non esiste o è scaduto
	 */
	@Transactional
	public void resetPassword(ResetPasswordRequest request) {

		PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenHasher.hash(request.token()))
			.orElseThrow(() -> new InvalidPasswordResetTokenException());

		if (token.isExpired(Instant.now(clock))) {
			throw new InvalidPasswordResetTokenException();
		}

		User user = token.getUser();
		user.changePassword(passwordEncoder.encode(request.newPassword()));

		passwordResetTokenRepository.deleteAllByUserId(user.getId());
		refreshTokenRepository.deleteAllByUserId(user.getId());
	}

	/**
	 * Conferma l'email dell'utente con il token ricevuto via email.
	 *
	 * @throws InvalidEmailVerificationTokenException se il token non esiste o è scaduto
	 */
	@Transactional
	public void verifyEmail(VerifyEmailRequest request) {

		Instant now = Instant.now(clock);

		EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(tokenHasher.hash(request.token()))
			.orElseThrow(() -> new InvalidEmailVerificationTokenException());

		if (token.isExpired(now)) {
			throw new InvalidEmailVerificationTokenException();
		}

		User user = token.getUser();
		user.markEmailVerified(now);

		emailVerificationTokenRepository.deleteAllByUserId(user.getId());
	}

	/**
	 * Invia un nuovo link di verifica all'utente autenticato. Il link precedente smette di funzionare.
	 *
	 * @param userId id dell'utente, dal campo {@code sub} del JWT
	 * @throws UserNotFoundException         se l'utente non esiste
	 * @throws EmailAlreadyVerifiedException se l'email è già verificata
	 */
	@Transactional
	public void resendEmailVerification(Integer userId) {

		User user = userService.getById(userId);
		if (user.isEmailVerified()) {
			throw new EmailAlreadyVerifiedException();
		}

		sendEmailVerification(user, Instant.now(clock));
	}

	// Crea un refresh token (nel database solo l'hash) e lo restituisce in chiaro insieme all'access token.
	private TokenResponse issueTokens(User user, Instant now) {
		String token = tokenHasher.generateToken();
		refreshTokenRepository.save(new RefreshToken(user, tokenHasher.hash(token), now.plus(securityProperties.refreshTokenTtl())));
		return TokenResponse.bearer(jwtService.createAccessToken(user), token, jwtService.getAccessTokenExpiresInSeconds());
	}

	// Crea un nuovo token di reset (cancellando i precedenti) e invia l'email.
	private void sendPasswordReset(User user, Instant now) {
		passwordResetTokenRepository.deleteAllByUserId(user.getId());

		String token = tokenHasher.generateToken();
		passwordResetTokenRepository.save(new PasswordResetToken(user, tokenHasher.hash(token), now.plus(securityProperties.passwordResetTokenTtl())));
		emailSender.sendPasswordReset(user.getEmail(), token);
	}

}
