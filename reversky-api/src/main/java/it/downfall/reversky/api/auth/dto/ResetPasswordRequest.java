package it.downfall.reversky.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Nuova password con il token ricevuto via email (POST /api/v1/auth/password/reset).
 * <p>
 * La nuova password segue le stesse regole della registrazione: se cambiano, vanno cambiate
 * anche in {@link RegisterRequest}.
 */
public record ResetPasswordRequest(

	@Schema(example = "Xy7pQ2...")
	@NotBlank(message = "Token is required")
	String token,

	@Schema(example = "UnaNuovaPassword2")
	@NotBlank(message = "Password is required")
	@Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one number")
	String newPassword
) {

	// Token e password non devono finire nei log
	@Override
	public String toString() {
		return "ResetPasswordRequest[token=****, newPassword=****]";
	}
}
