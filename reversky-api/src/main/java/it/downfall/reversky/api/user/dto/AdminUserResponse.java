package it.downfall.reversky.api.user.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.user.entity.User;

/**
 * Un utente visto dal backoffice (GET /api/v1/admin/users e azioni su un utente, docs/backoffice).
 *
 * @param credits saldo dell'inventario, letto da {@code InventoryService}
 */
public record AdminUserResponse(

	@Schema(example = "42")
	Integer id,

	@Schema(example = "pioggia_inversa")
	String username,

	@Schema(example = "mario@example.com")
	String email,

	@Schema(example = "USER")
	String role,

	@Schema(example = "true")
	boolean enabled,

	@Schema(example = "false")
	boolean emailVerified,

	@Schema(example = "350")
	int credits,

	@Schema(example = "2026-11-21T10:00:00Z")
	Instant createdAt
) {

	/**
	 * Converte l'entità nel DTO. Il ruolo è LAZY: va chiamato dentro la transazione del service.
	 */
	public static AdminUserResponse from(User user, int credits) {
		return new AdminUserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole().getName(),
			user.isEnabled(), user.isEmailVerified(), credits, user.getCreatedAt());
	}
}
