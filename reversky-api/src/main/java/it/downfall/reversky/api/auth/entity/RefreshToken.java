package it.downfall.reversky.api.auth.entity;

import java.time.Instant;

import it.downfall.reversky.api.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Refresh token (durata 30 giorni). Serve a ottenere un nuovo access token senza rifare il login;
 * a ogni rinnovo viene eliminato e sostituito da uno nuovo (rotazione).
 */
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends AbstractUserToken {

	public RefreshToken(User user, String tokenHash, Instant expiresAt) {
		super(user, tokenHash, expiresAt);
	}
}
