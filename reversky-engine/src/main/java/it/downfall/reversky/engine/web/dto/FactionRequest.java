package it.downfall.reversky.engine.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Una fazione della tabella {@code factions} (docs/fazioni).
 *
 * @param id   id della fazione
 * @param name nome della fazione, in inglese
 */
public record FactionRequest(

		@NotNull(message = "Faction id is required")
		Integer id,

		@NotBlank(message = "Faction name is required")
		String name) {
}
