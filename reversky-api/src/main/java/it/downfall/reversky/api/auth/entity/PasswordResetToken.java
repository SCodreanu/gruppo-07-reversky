package it.downfall.reversky.api.auth.entity;

import java.time.Instant;

import it.downfall.reversky.api.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Token per il reset della password (durata 30 minuti). Si elimina dopo l’uso.
 */
@Entity
@Table(name = "password_reset_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken extends AbstractUserToken {

	public PasswordResetToken(User user, String tokenHash, Instant expiresAt) {
		super(user, tokenHash, expiresAt);
	}
}
