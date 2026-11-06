package it.downfall.reversky.api.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.service.UserService;

/**
 * Gli endpoint di {@link AuthController} via HTTP, con i filtri di Spring Security attivi.
 * <p>
 * La logica dei flussi è già coperta da {@code AuthServiceIT}: qui si controlla quello che il service
 * non vede, cioè status HTTP, corpo JSON, formato ProblemDetail, validazione con {@code @Valid}
 * e regole di {@code SecurityConfig}. Come in AuthServiceIT ogni test crea un proprio utente
 * e {@link EmailSender} è un mock da cui si leggono i token in chiaro.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT {

	private static final String PASSWORD = "CorrectPassword1";
	private static final String NEW_PASSWORD = "BrandNewPassword2";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserService userService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private EmailSender emailSender;

	// --- register ---

	@Test
	@DisplayName("Valid registration returns 201 without body")
	void register_validData_returns201WithoutBody() throws Exception {
		// Arrange
		String suffix = randomSuffix();
		String body = registerBody("user_" + suffix, suffix + "@example.com", PASSWORD, true);

		// Act + Assert
		postJson("/api/v1/auth/register", body)
			.andExpect(status().isCreated())
			.andExpect(content().string(""));
	}

	@Test
	@DisplayName("Invalid fields return 400 with the list of wrong fields")
	void register_invalidFields_returns400WithFieldErrors() throws Exception {
		// Arrange: password troppo corta e termini non accettati
		String suffix = randomSuffix();
		String body = registerBody("user_" + suffix, suffix + "@example.com", "short", false);

		// Act + Assert
		postJson("/api/v1/auth/register", body)
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItems("password", "acceptTerms")));
	}

	@Test
	@DisplayName("A username already in use returns 409")
	void register_usernameTaken_returns409() throws Exception {
		// Arrange
		User existing = createUser();
		String body = registerBody(existing.getUsername(), randomSuffix() + "@example.com", PASSWORD, true);

		// Act + Assert
		postJson("/api/v1/auth/register", body)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("AUTH_USERNAME_TAKEN"));
	}

	// --- login ---

	@Test
	@DisplayName("Correct credentials return 200 with a Bearer token pair")
	void login_correctCredentials_returns200WithTokens() throws Exception {
		// Arrange
		User user = createUser();

		// Act + Assert
		postJson("/api/v1/auth/login", loginBody(user.getEmail(), PASSWORD))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.refreshToken").isNotEmpty())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(900));
	}

	@Test
	@DisplayName("A wrong password returns 401 in ProblemDetail format")
	void login_wrongPassword_returns401() throws Exception {
		// Arrange
		User user = createUser();

		// Act + Assert
		postJson("/api/v1/auth/login", loginBody(user.getEmail(), "WrongPassword1"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
			.andExpect(jsonPath("$.instance").value("/api/v1/auth/login"));
	}

	@Test
	@DisplayName("Missing email and password return 400")
	void login_missingFields_returns400() throws Exception {
		// Act + Assert
		postJson("/api/v1/auth/login", "{}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItems("email", "password")));
	}

	@Test
	@DisplayName("Malformed JSON returns 400, not 500")
	void login_malformedJson_returns400() throws Exception {
		// Act + Assert
		postJson("/api/v1/auth/login", "{\"email\": ")
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("A disabled account returns 403")
	void login_disabledAccount_returns403() throws Exception {
		// Arrange
		User user = createUser();
		disable(user);

		// Act + Assert
		postJson("/api/v1/auth/login", loginBody(user.getEmail(), PASSWORD))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("AUTH_ACCOUNT_DISABLED"));
	}

	// --- refresh ---

	@Test
	@DisplayName("A valid refresh token returns 200 with a new refresh token")
	void refresh_validToken_returns200WithNewToken() throws Exception {
		// Arrange
		String refreshToken = refreshTokenOf(createUser());

		// Act + Assert
		postJson("/api/v1/auth/refresh", refreshBody(refreshToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.refreshToken").value(not(refreshToken)));
	}

	@Test
	@DisplayName("A refresh token already used returns 401")
	void refresh_reusedToken_returns401() throws Exception {
		// Arrange
		String refreshToken = refreshTokenOf(createUser());
		postJson("/api/v1/auth/refresh", refreshBody(refreshToken)).andExpect(status().isOk());

		// Act + Assert
		postJson("/api/v1/auth/refresh", refreshBody(refreshToken))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REFRESH_TOKEN_INVALID"));
	}

	// --- logout ---

	@Test
	@DisplayName("Logout returns 204 and the refresh token stops working")
	void logout_validToken_returns204AndTokenIsRevoked() throws Exception {
		// Arrange
		String refreshToken = refreshTokenOf(createUser());

		// Act
		postJson("/api/v1/auth/logout", refreshBody(refreshToken))
			.andExpect(status().isNoContent());

		// Assert
		postJson("/api/v1/auth/refresh", refreshBody(refreshToken))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Logout with an unknown token still returns 204")
	void logout_unknownToken_returns204() throws Exception {
		// Act + Assert
		postJson("/api/v1/auth/logout", refreshBody("unknown-token"))
			.andExpect(status().isNoContent());
	}

	// --- forgot / reset password ---

	@Test
	@DisplayName("Forgot password with an unknown email still returns 204")
	void forgotPassword_unknownEmail_returns204() throws Exception {
		// Act + Assert
		postJson("/api/v1/auth/password/forgot", "{\"email\": \"nobody-%s@example.com\"}".formatted(randomSuffix()))
			.andExpect(status().isNoContent());

		verify(emailSender, never()).sendPasswordReset(anyString(), anyString());
	}

	@Test
	@DisplayName("A valid reset token returns 204 and the new password works")
	void resetPassword_validToken_returns204AndNewPasswordWorks() throws Exception {
		// Arrange
		User user = createUser();
		postJson("/api/v1/auth/password/forgot", "{\"email\": \"%s\"}".formatted(user.getEmail()))
			.andExpect(status().isNoContent());
		String resetToken = lastResetToken(user);

		// Act
		postJson("/api/v1/auth/password/reset", resetBody(resetToken, NEW_PASSWORD))
			.andExpect(status().isNoContent());

		// Assert
		postJson("/api/v1/auth/login", loginBody(user.getEmail(), NEW_PASSWORD))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("An invalid reset token returns 400")
	void resetPassword_invalidToken_returns400() throws Exception {
		// Act + Assert
		postJson("/api/v1/auth/password/reset", resetBody("invalid-token", NEW_PASSWORD))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("AUTH_RESET_TOKEN_INVALID"));
	}

	// --- verify email ---

	@Test
	@DisplayName("A valid verification token returns 204 and marks the email as verified")
	void verifyEmail_validToken_returns204() throws Exception {
		// Arrange: il reinvio genera un token di verifica e lo passa al mock
		User user = createUser();
		resend(accessTokenOf(user)).andExpect(status().isNoContent());
		String verificationToken = lastVerificationToken(user);

		// Act
		postJson("/api/v1/auth/email/verify", tokenBody(verificationToken))
			.andExpect(status().isNoContent());

		// Assert
		assertThat(userService.getById(user.getId()).isEmailVerified()).isTrue();
	}

	@Test
	@DisplayName("An invalid verification token returns 400")
	void verifyEmail_invalidToken_returns400() throws Exception {
		// Act + Assert
		postJson("/api/v1/auth/email/verify", tokenBody("invalid-token"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("AUTH_VERIFICATION_TOKEN_INVALID"));
	}

	// --- resend verification (unico endpoint di /auth che richiede il token) ---

	@Test
	@DisplayName("Resend without an access token returns 401 with WWW-Authenticate")
	void resend_withoutToken_returns401() throws Exception {
		// Act + Assert
		mockMvc.perform(post("/api/v1/auth/email/resend"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("Resend with an invalid access token returns 401")
	void resend_invalidToken_returns401() throws Exception {
		// Act + Assert
		resend("not-a-jwt")
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("Resend for an unverified user returns 204 and sends the email")
	void resend_unverifiedUser_returns204() throws Exception {
		// Arrange
		User user = createUser();

		// Act
		resend(accessTokenOf(user))
			.andExpect(status().isNoContent());

		// Assert
		verify(emailSender).sendEmailVerification(eq(user.getEmail()), any());
	}

	@Test
	@DisplayName("Resend for a user already verified returns 409")
	void resend_verifiedUser_returns409() throws Exception {
		// Arrange
		User user = createUser();
		markVerified(user);

		// Act + Assert
		resend(accessTokenOf(user))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("AUTH_EMAIL_ALREADY_VERIFIED"));
	}

	// --- regole generali di SecurityConfig ---

	@Test
	@DisplayName("A protected endpoint without token returns 401")
	void protectedEndpoint_withoutToken_returns401() throws Exception {
		// Act + Assert: /users non ha una regola propria in SecurityConfig, vale anyRequest().authenticated()
		mockMvc.perform(get("/api/v1/users/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("A USER on an admin endpoint returns 403")
	void adminEndpoint_userRole_returns403() throws Exception {
		// Arrange
		String accessToken = accessTokenOf(createUser());

		// Act + Assert
		mockMvc.perform(get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	// --- documentazione OpenAPI ---

	@Test
	@DisplayName("OpenAPI declares the Bearer scheme and requires it only on protected endpoints")
	void apiDocs_bearerScheme_appliedOnlyToResend() throws Exception {
		// Act + Assert: è il JSON che legge Swagger UI, quindi quello che vede il frontend
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
			.andExpect(jsonPath("$.paths['/api/v1/auth/email/resend'].post.security[0]").value(hasKey("bearerAuth")))
			.andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security").doesNotExist());
	}

	// --- helper ---

	private ResultActions postJson(String path, String json) throws Exception {
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions resend(String accessToken) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/email/resend").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
	}

	// Fa il login via HTTP e restituisce il JSON della risposta
	private String loginResponse(User user) throws Exception {
		return postJson("/api/v1/auth/login", loginBody(user.getEmail(), PASSWORD))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private String accessTokenOf(User user) throws Exception {
		return JsonPath.read(loginResponse(user), "$.accessToken");
	}

	private String refreshTokenOf(User user) throws Exception {
		return JsonPath.read(loginResponse(user), "$.refreshToken");
	}

	// Token in chiaro dell'ultima email di reset inviata all'utente
	private String lastResetToken(User user) {
		ArgumentCaptor<String> rawToken = ArgumentCaptor.forClass(String.class);
		verify(emailSender).sendPasswordReset(eq(user.getEmail()), rawToken.capture());

		return rawToken.getValue();
	}

	// Token in chiaro dell'ultima email di verifica inviata all'utente
	private String lastVerificationToken(User user) {
		ArgumentCaptor<String> rawToken = ArgumentCaptor.forClass(String.class);
		verify(emailSender).sendEmailVerification(eq(user.getEmail()), rawToken.capture());

		return rawToken.getValue();
	}

	private User createUser() {
		String suffix = randomSuffix();

		return userService.createUser("user_" + suffix, suffix + "@example.com", passwordEncoder.encode(PASSWORD), Instant.now());
	}

	// SQL diretto: User non ha ancora un metodo per disabilitare, arriverà con il backoffice
	private void disable(User user) {
		jdbcTemplate.update("UPDATE users SET is_enabled = false WHERE id = ?", user.getId());
	}

	private void markVerified(User user) {
		jdbcTemplate.update("UPDATE users SET email_verified_at = now() WHERE id = ?", user.getId());
	}

	private static String randomSuffix() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	private static String registerBody(String username, String email, String password, boolean acceptTerms) {
		return """
			{"username": "%s", "email": "%s", "password": "%s", "acceptPrivacy": true, "acceptTerms": %s}
			""".formatted(username, email, password, acceptTerms);
	}

	private static String loginBody(String email, String password) {
		return "{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password);
	}

	private static String refreshBody(String refreshToken) {
		return "{\"refreshToken\": \"%s\"}".formatted(refreshToken);
	}

	private static String resetBody(String token, String newPassword) {
		return "{\"token\": \"%s\", \"newPassword\": \"%s\"}".formatted(token, newPassword);
	}

	private static String tokenBody(String token) {
		return "{\"token\": \"%s\"}".formatted(token);
	}
}
