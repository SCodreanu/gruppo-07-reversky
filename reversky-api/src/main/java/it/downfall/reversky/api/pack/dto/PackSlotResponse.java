package it.downfall.reversky.api.pack.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.dto.RaritySummaryResponse;
import it.downfall.reversky.api.pack.entity.PackRarityRule;

/**
 * Uno slot base del pacchetto.
 */
public record PackSlotResponse(

	RaritySummaryResponse rarity,

	@Schema(example = "3")
	int quantity
) {

	public static PackSlotResponse from(PackRarityRule rule) {
		return new PackSlotResponse(RaritySummaryResponse.from(rule.getRarity()), rule.getQuantity());
	}
}
