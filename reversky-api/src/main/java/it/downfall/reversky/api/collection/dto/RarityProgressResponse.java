package it.downfall.reversky.api.collection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.dto.RaritySummaryResponse;

/**
 * Avanzamento della collezione in una rarità ({@link CollectionSummaryResponse}).
 */
public record RarityProgressResponse(

	RaritySummaryResponse rarity,

	@Schema(example = "12", description = "Active original cards of this rarity that I own, directly or through a variant")
	int owned,

	@Schema(example = "28", description = "Active original cards of this rarity in the catalog")
	int total
) {
}
