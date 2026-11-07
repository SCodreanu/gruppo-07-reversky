package it.downfall.reversky.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dati di una rarità per PUT /admin/rarities/{id} (docs/backoffice).
 * <p>
 * Come {@link RarityCreateRequest}, ma senza sortingOrder: si sceglie alla creazione e non cambia più,
 * perché arriva all'engine come rarityOrder. Se il client lo invia comunque, viene ignorato.
 */
public record RarityUpdateRequest(

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
	Integer maxDef
) {
}
