package it.downfall.reversky.api.deck.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.deck.entity.Deck;

/**
 * Un mazzo nella lista dei miei mazzi (GET /api/v1/decks), senza le carte.
 *
 * @param cardCount numero totale di carte, sommando le copie
 * @param valid     ricalcolato a ogni lettura: un mazzo salvato può diventare non valido (regole o carte cambiate)
 */
public record DeckSummaryResponse(

	@Schema(example = "8")
	Integer id,

	@Schema(example = "Pioggia di Archivio")
	String name,

	@Schema(example = "30")
	int cardCount,

	@Schema(example = "true")
	boolean valid
) {

	public static DeckSummaryResponse from(Deck deck, boolean valid) {
		return new DeckSummaryResponse(deck.getId(), deck.getName(), deck.getCardCount(), valid);
	}
}
