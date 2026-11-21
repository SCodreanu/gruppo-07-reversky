package it.downfall.reversky.api.deck.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Una regola del mazzo non rispettata (docs/mazzi, "Come si valida").
 *
 * @param code    codice {@code DECK_*} per il frontend
 * @param message spiegazione in inglese per il giocatore
 * @param cardId  carta coinvolta, oppure null per le regole sull'intero mazzo
 */
public record DeckViolation(

	@Schema(example = "DECK_COPY_LIMIT")
	String code,

	@Schema(example = "Too many copies of 'Hounds' (2/1)")
	String message,

	@Schema(example = "51", nullable = true)
	Integer cardId
) {
}
