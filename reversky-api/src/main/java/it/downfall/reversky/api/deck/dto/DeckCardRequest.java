package it.downfall.reversky.api.deck.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Una carta del mazzo con le sue copie.
 */
public record DeckCardRequest(

	@Schema(example = "4")
	@NotNull(message = "Card id is required")
	Integer cardId,

	@Schema(example = "3")
	@NotNull(message = "Quantity is required")
	@Min(value = 1, message = "Quantity must be at least 1")
	Integer quantity
) {
}
