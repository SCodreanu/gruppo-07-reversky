package it.downfall.reversky.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Token ricevuto via email per verificare l'indirizzo (POST /api/v1/auth/email/verify).
 */
public record VerifyEmailRequest(

	@Schema(example = "Xy7pQ2...")
	@NotBlank(message = "Token is required")
	String token
) {

	// Il token non deve finire nei log
	@Override
	public String toString() {
		return "VerifyEmailRequest[token=****]";
	}
}
