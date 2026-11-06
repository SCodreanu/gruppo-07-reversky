package it.downfall.reversky.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Refresh token inviato dal client per il rinnovo (POST /api/v1/auth/refresh)
 * e per il logout (POST /api/v1/auth/logout).
 */
public record RefreshTokenRequest(

	@Schema(example = "q8Zk1x3H...")
	@NotBlank(message = "Refresh token is required")
	String refreshToken
) {

	// Il token è una credenziale: non deve finire nei log
	@Override
	public String toString() {
		return "RefreshTokenRequest[refreshToken=****]";
	}
}
