package it.downfall.reversky.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Risposta di login e rinnovo: access token JWT e nuovo refresh token (docs/autenticazione).
 *
 * @param accessToken  JWT da inviare nell'header {@code Authorization: Bearer ...}
 * @param refreshToken token per ottenere un nuovo access token (ruota a ogni rinnovo)
 * @param tokenType    sempre {@code Bearer}
 * @param expiresIn    durata dell'access token in secondi
 */
public record TokenResponse(

	@Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
	String accessToken,

	@Schema(example = "q8Zk1x3H...")
	String refreshToken,

	@Schema(example = "Bearer")
	String tokenType,

	@Schema(example = "900")
	long expiresIn
) {

	private static final String BEARER = "Bearer";

	/**
	 * Crea la risposta con tipo {@code Bearer}.
	 */
	public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
		return new TokenResponse(accessToken, refreshToken, BEARER, expiresIn);
	}

	// I token sono credenziali: non devono finire nei log
	@Override
	public String toString() {
		return "TokenResponse[accessToken=****, refreshToken=****, tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
	}
}
