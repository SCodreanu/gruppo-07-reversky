package it.downfall.reversky.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.user.entity.User;

/**
 * Profilo dell'utente autenticato (GET /api/v1/users/me, docs/autenticazione).
 *
 * @param emailVerified false finché l'utente non conferma l'email: il frontend mostra il pulsante di reinvio
 */
public record UserResponse(

	@Schema(example = "42")
	Integer id,

	@Schema(example = "pioggia_inversa")
	String username,

	@Schema(example = "mario@example.com")
	String email,

	@Schema(example = "USER")
	String role,

	@Schema(example = "false")
	boolean emailVerified
) {

	// TODO(@tommasofatticcioni): aggiungere i crediti quando esisterà inventories (docs/crediti)

	/**
	 * Converte l'entità nel DTO. Il ruolo è LAZY: va chiamato dentro la transazione del service.
	 */
	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole().getName(), user.isEmailVerified());
	}
}
