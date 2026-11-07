package it.downfall.reversky.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Dati di una nuova rarità per POST /admin/rarities (docs/backoffice).
 * <p>
 * I massimi vuoti significano "nessun limite". sortingOrder si sceglie qui e non cambia più.
 */
public record RarityCreateRequest(

	@Schema(example = "Rare")
	@NotBlank(message = "Name is required")
	@Size(max = 100, message = "Name must not exceed 100 characters")
	String name,

	@Schema(example = "2")
	@NotNull(message = "Deck limit is required")
	@Min(value = 1, message = "Deck limit must be at least 1")
	Integer deckLimit,

	@Schema(example = "5", description = "Empty means no limit")
	@Min(value = 0, message = "Max per deck must not be negative")
	Integer maxPerDeck,

	@Schema(example = "10", description = "Empty means no limit")
	@Min(value = 0, message = "Max ATK must not be negative")
	Integer maxAtk,

	@Schema(example = "10", description = "Empty means no limit")
	@Min(value = 1, message = "Max DEF must be at least 1")
	Integer maxDef,

	@Schema(example = "3", description = "Position from common to rare; cannot be changed later")
	@NotNull(message = "Sorting order is required")
	@Positive(message = "Sorting order must be positive")
	Integer sortingOrder
) {
}
