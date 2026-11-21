package it.downfall.reversky.api.deck.dto;

import java.util.Comparator;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.deck.entity.Deck;

/**
 * Un mio mazzo con le sue carte (GET /api/v1/decks/{id}, risposta di POST e PUT, docs/mazzi).
 *
 * @param cardCount numero totale di carte, sommando le copie
 * @param cards     le carte in ordine di id
 */
public record DeckResponse(

	@Schema(example = "8")
	Integer id,

	@Schema(example = "Pioggia di Archivio")
	String name,

	@Schema(example = "30")
	int cardCount,

	List<DeckCardResponse> cards
) {

	/**
	 * Converte l'entità nel DTO. Carte, rarità e fazioni sono LAZY: va chiamato dentro la transazione del service.
	 */
	public static DeckResponse from(Deck deck) {
		List<DeckCardResponse> cards = deck.getCards().stream()
			.sorted(Comparator.comparing(row -> row.getCardId()))
			.map(DeckCardResponse::from)
			.toList();
		return new DeckResponse(deck.getId(), deck.getName(), deck.getCardCount(), cards);
	}
}
