package it.downfall.reversky.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dati per la registrazione (POST /api/v1/auth/register).
 */
public record RegisterRequest(

	@Schema(example = "pioggia_inversa")
	@NotBlank(message = "Username is required")
	@Pattern(regexp = "^[A-Za-z0-9_]{3,30}$", message = "Username must be 3 to 30 characters long: letters, numbers or _")
	String username,

	@Schema(example = "mario@example.com")
	@NotBlank(message = "Email is required")
	@Email(message = "Invalid email format")
	@Size(max = 255, message = "Email must not exceed 255 characters")
	String email,

	@Schema(example = "UnaPasswordSicura1")
	@NotBlank(message = "Password is required")
	@Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one number")
	String password,

	@AssertTrue(message = "You must accept the privacy policy")
	boolean acceptPrivacy,

	@AssertTrue(message = "You must accept the terms of service")
	boolean acceptTerms
) {

	@Override
	public String toString() {
		return "RegisterRequest[username=" + username + ", email=" + email + ", password=****]";
	}
}
