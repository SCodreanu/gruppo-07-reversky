package it.downfall.reversky.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Richiesta del link per reimpostare la password (POST /api/v1/auth/password/forgot).
 * <p>
 * La risposta è sempre 204, anche se l'email non è registrata (docs/autenticazione).
 */
public record ForgotPasswordRequest(

	@Schema(example = "mario@example.com")
	@NotBlank(message = "Email is required")
	@Email(message = "Invalid email format")
	String email
) {
}
