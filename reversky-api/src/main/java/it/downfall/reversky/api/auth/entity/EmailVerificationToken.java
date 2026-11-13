package it.downfall.reversky.api.auth.entity;

import java.time.Instant;

import it.downfall.reversky.api.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Token per la verifica dell’email. Dopo l’uso si elimina e si imposta {@code users.email_verified_at}.
 */
@Entity
@Table(name = "email_verification_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerificationToken extends AbstractUserToken {

	public EmailVerificationToken(User user, String tokenHash, Instant expiresAt) {
		super(user, tokenHash, expiresAt);
	}
}
