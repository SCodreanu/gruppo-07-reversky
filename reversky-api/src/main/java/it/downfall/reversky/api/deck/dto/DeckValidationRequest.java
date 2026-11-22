package it.downfall.reversky.api.deck.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Contenuto da controllare senza salvarlo (POST /api/v1/decks/validate, per il deck builder).
 */
public record DeckValidationRequest(

	@NotNull(message = "Cards are required")
	List<@NotNull(message = "Card is required") @Valid DeckCardRequest> cards
) {

	// Stessa regola di DeckRequest: l'errore arriva sul campo "cardIdsUnique"
	@Schema(hidden = true)
	@AssertTrue(message = "Each card must appear only once: use quantity for more copies")
	public boolean isCardIdsUnique() {
		return DeckRequest.hasUniqueCardIds(cards);
	}
}
