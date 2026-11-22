package it.downfall.reversky.api.deck.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.dto.CatalogCardResponse;
import it.downfall.reversky.api.deck.entity.DeckCard;

/**
 * Una carta del mazzo, con tutti i dati del catalogo per il deck builder.
 */
public record DeckCardResponse(

	CatalogCardResponse card,

	@Schema(example = "3")
	int quantity
) {

	public static DeckCardResponse from(DeckCard row) {
		return new DeckCardResponse(CatalogCardResponse.from(row.getCard()), row.getQuantity());
	}
}
