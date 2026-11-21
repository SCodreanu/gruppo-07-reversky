package it.downfall.reversky.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Nuovo ruolo di un utente (PUT /api/v1/admin/users/{id}/role, docs/backoffice).
 */
public record RoleChangeRequest(

	@Schema(example = "ADMIN", allowableValues = {"USER", "ADMIN"})
	@NotBlank(message = "Role is required")
	@Pattern(regexp = "USER|ADMIN", message = "Role must be USER or ADMIN")
	String role
) {
}
