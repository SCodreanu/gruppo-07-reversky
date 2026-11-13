package it.downfall.reversky.api.auth.job;

import java.time.Clock;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.auth.repository.EmailVerificationTokenRepository;
import it.downfall.reversky.api.auth.repository.PasswordResetTokenRepository;
import it.downfall.reversky.api.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Elimina ogni notte i token scaduti dalle tre tabelle (docs/autenticazione, "Pulizia").
 * <p>
 * I token scaduti sono già inutilizzabili (il service controlla {@code isExpired}): il job serve solo
 * a non far crescere le tabelle. Gli indici su {@code expires_at} rendono veloce la cancellazione.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupJob {

	private final EmailVerificationTokenRepository emailVerificationTokenRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final RefreshTokenRepository refreshTokenRepository;

	private final Clock clock;

	/**
	 * Ogni giorno alle 3:00, quando il traffico è più basso.
	 */
	@Scheduled(cron = "0 0 3 * * *")
	@Transactional
	public void deleteExpiredTokens() {

		Instant now = Instant.now(clock);

		int verificationDeleted = emailVerificationTokenRepository.deleteExpired(now);
		int resetDeleted = passwordResetTokenRepository.deleteExpired(now);
		int refreshDeleted = refreshTokenRepository.deleteExpired(now);

		log.info("Deleted expired tokens: verification={}, reset={}, refresh={}", verificationDeleted, resetDeleted, refreshDeleted);
	}
}
