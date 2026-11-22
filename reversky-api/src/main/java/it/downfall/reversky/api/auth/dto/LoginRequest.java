package it.downfall.reversky.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenziali per il login (POST /api/v1/auth/login).
 * <p>
 * Si controlla solo che i campi ci siano: un'email malformata o una password sbagliata danno
 * comunque lo stesso 401 generico, senza dire quale dei due è errato (docs/autenticazione).
 */
public record LoginRequest(

	@Schema(example = "mario@example.com")
	@NotBlank(message = "Email is required")
	String email,

	@Schema(example = "UnaPasswordSicura1")
	@NotBlank(message = "Password is required")
	String password
) {

	// Il toString() di un record stampa tutti i campi: la password non deve finire nei log
	@Override
	public String toString() {
		return "LoginRequest[email=" + email + ", password=****]";
	}
}
