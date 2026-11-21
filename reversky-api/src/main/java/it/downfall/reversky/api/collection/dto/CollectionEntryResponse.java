package it.downfall.reversky.api.collection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.dto.CatalogCardResponse;

/**
 * Una carta della mia collezione (GET /collection, docs/collezione).
 */
public record CollectionEntryResponse(

	@Schema(description = "The card, in the same shape as GET /cards/{id}; it may be deactivated or a variant")
	CatalogCardResponse card,

	@Schema(example = "3", description = "Copies owned, always at least 1")
	int quantity,

	@Schema(example = "1", description = "Copies offered in my open trades where I made the current offer: they can't "
		+ "be offered elsewhere or used in a deck until the trade closes")
	int reservedQuantity,

	@Schema(example = "2", description = "Number of my decks that contain this card")
	int usedInDecks
) {
}
