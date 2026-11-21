package it.downfall.reversky.api.deck.dto;

import java.util.Comparator;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.deck.entity.Deck;

/**
 * Un mio mazzo con le sue carte (GET /api/v1/decks/{id}, risposta di POST e PUT, docs/mazzi).
 *
 * @param cardCount  numero totale di carte, sommando le copie
 * @param valid      ricalcolato a ogni lettura: un mazzo salvato può diventare non valido
 * @param cards      le carte in ordine di id
 * @param violations le regole non rispettate oggi, lista vuota se il mazzo è valido
 */
public record DeckResponse(

	@Schema(example = "8")
	Integer id,

	@Schema(example = "Pioggia di Archivio")
	String name,

	@Schema(example = "30")
	int cardCount,

	@Schema(example = "true")
	boolean valid,

	List<DeckCardResponse> cards,

	List<DeckViolation> violations
) {

	/**
	 * Converte l'entità nel DTO. Carte, rarità e fazioni sono LAZY: va chiamato dentro la transazione del service.
	 */
	public static DeckResponse from(Deck deck, List<DeckViolation> violations) {
		List<DeckCardResponse> cards = deck.getCards().stream()
			.sorted(Comparator.comparing(row -> row.getCardId()))
			.map(DeckCardResponse::from)
			.toList();
		return new DeckResponse(deck.getId(), deck.getName(), deck.getCardCount(), violations.isEmpty(), cards, violations);
	}
}
