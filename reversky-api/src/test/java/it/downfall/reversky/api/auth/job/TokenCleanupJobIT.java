package it.downfall.reversky.api.auth.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.auth.entity.EmailVerificationToken;
import it.downfall.reversky.api.auth.entity.PasswordResetToken;
import it.downfall.reversky.api.auth.entity.RefreshToken;
import it.downfall.reversky.api.auth.repository.EmailVerificationTokenRepository;
import it.downfall.reversky.api.auth.repository.PasswordResetTokenRepository;
import it.downfall.reversky.api.auth.repository.RefreshTokenRepository;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.TokenHasher;
import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.service.UserService;

/**
 * {@link TokenCleanupJob} su un PostgreSQL vero: il metodo si chiama direttamente,
 * senza aspettare il cron.
 * <p>
 * Il job cancella i token scaduti di tutti gli utenti, quindi ogni test controlla solo i propri
 * token, cercandoli per hash.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TokenCleanupJobIT {

	private static final Duration ONE_HOUR = Duration.ofHours(1);

	@Autowired
	private TokenCleanupJob tokenCleanupJob;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordResetTokenRepository passwordResetTokenRepository;

	@Autowired
	private EmailVerificationTokenRepository emailVerificationTokenRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TokenHasher tokenHasher;

	// Non serve al test: con la stessa configurazione di AuthServiceIT, Spring riusa
	// il contesto già avviato (e il suo container) invece di crearne uno nuovo.
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("Expired refresh tokens are deleted, valid ones are kept")
	void deleteExpiredTokens_refreshTokens_deletesOnlyExpired() {
		// Arrange
		User user = createUser();
		String expiredHash = newHash();
		String validHash = newHash();
		refreshTokenRepository.save(new RefreshToken(user, expiredHash, Instant.now().minus(ONE_HOUR)));
		refreshTokenRepository.save(new RefreshToken(user, validHash, Instant.now().plus(ONE_HOUR)));

		// Act
		tokenCleanupJob.deleteExpiredTokens();

		// Assert
		assertThat(refreshTokenRepository.findByTokenHash(expiredHash)).isEmpty();
		assertThat(refreshTokenRepository.findByTokenHash(validHash)).isPresent();
	}

	@Test
	@DisplayName("Expired password reset tokens are deleted, valid ones are kept")
	void deleteExpiredTokens_passwordResetTokens_deletesOnlyExpired() {
		// Arrange
		User user = createUser();
		String expiredHash = newHash();
		String validHash = newHash();
		passwordResetTokenRepository.save(new PasswordResetToken(user, expiredHash, Instant.now().minus(ONE_HOUR)));
		passwordResetTokenRepository.save(new PasswordResetToken(user, validHash, Instant.now().plus(ONE_HOUR)));

		// Act
		tokenCleanupJob.deleteExpiredTokens();

		// Assert
		assertThat(passwordResetTokenRepository.findByTokenHash(expiredHash)).isEmpty();
		assertThat(passwordResetTokenRepository.findByTokenHash(validHash)).isPresent();
	}

	@Test
	@DisplayName("Expired email verification tokens are deleted, valid ones are kept")
	void deleteExpiredTokens_emailVerificationTokens_deletesOnlyExpired() {
		// Arrange
		User user = createUser();
		String expiredHash = newHash();
		String validHash = newHash();
		emailVerificationTokenRepository.save(new EmailVerificationToken(user, expiredHash, Instant.now().minus(ONE_HOUR)));
		emailVerificationTokenRepository.save(new EmailVerificationToken(user, validHash, Instant.now().plus(ONE_HOUR)));

		// Act
		tokenCleanupJob.deleteExpiredTokens();

		// Assert
		assertThat(emailVerificationTokenRepository.findByTokenHash(expiredHash)).isEmpty();
		assertThat(emailVerificationTokenRepository.findByTokenHash(validHash)).isPresent();
	}

	private User createUser() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);

		return userService.createUser("user_" + suffix, suffix + "@example.com", passwordEncoder.encode("CorrectPassword1"), Instant.now());
	}

	// Hash di un token casuale: stesso formato di quelli salvati dal service
	private String newHash() {
		return tokenHasher.hash(tokenHasher.generateToken());
	}
}
