package it.downfall.reversky.api.deck.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Creazione o modifica di un mazzo (POST /api/v1/decks, PUT /api/v1/decks/{id}, docs/mazzi).
 *
 * @param name 1-50 caratteri, univoco tra i mazzi dell'utente; gli spazi all'inizio e alla fine si tolgono
 */
public record DeckRequest(

	@Schema(example = "Pioggia di Archivio")
	@NotBlank(message = "Name is required")
	@Size(max = 50, message = "Name must be at most 50 characters")
	String name
) {
}
