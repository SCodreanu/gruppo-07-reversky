package it.downfall.reversky.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dati di una fazione per POST /admin/factions e PUT /admin/factions/{id} (docs/backoffice).
 */
public record FactionRequest(

	@Schema(example = "Theory of the Wound")
	@NotBlank(message = "Name is required")
	@Size(max = 100, message = "Name must not exceed 100 characters")
	String name,

	@Schema(example = "factions/wound.svg")
	@NotBlank(message = "Emblem path is required")
	String emblemPath,

	@Schema(example = "Those who survived the Backrooms carry its wounds.")
	String description
) {
}
