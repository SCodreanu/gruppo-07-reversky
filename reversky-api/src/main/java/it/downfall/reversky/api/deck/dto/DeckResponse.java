package it.downfall.reversky.api.deck.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.deck.entity.Deck;

/**
 * Un mazzo dell'utente (docs/mazzi).
 */
public record DeckResponse(

	@Schema(example = "8")
	Integer id,

	@Schema(example = "Pioggia di Archivio")
	String name
) {

	public static DeckResponse from(Deck deck) {
		return new DeckResponse(deck.getId(), deck.getName());
	}
}
