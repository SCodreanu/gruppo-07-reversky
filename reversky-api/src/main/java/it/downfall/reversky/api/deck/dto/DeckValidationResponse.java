package it.downfall.reversky.api.deck.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Esito di POST /api/v1/decks/validate: tutte le regole non rispettate, lista vuota se il mazzo è valido.
 */
public record DeckValidationResponse(

	@Schema(example = "false")
	boolean valid,

	List<DeckViolation> violations
) {

	public static DeckValidationResponse of(List<DeckViolation> violations) {
		return new DeckValidationResponse(violations.isEmpty(), violations);
	}
}
