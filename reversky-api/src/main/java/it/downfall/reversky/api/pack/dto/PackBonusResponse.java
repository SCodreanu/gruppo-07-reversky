package it.downfall.reversky.api.pack.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.dto.RaritySummaryResponse;
import it.downfall.reversky.api.pack.entity.PackBonusRule;

/**
 * La probabilità di una carta bonus, mostrata anche ai giocatori per trasparenza (docs/pacchetti).
 *
 * @param probabilityUnits unità su 10.000, come nel database
 * @param percentage       la stessa probabilità in percentuale, es. 2.5
 */
public record PackBonusResponse(

	RaritySummaryResponse rarity,

	@Schema(example = "250")
	int probabilityUnits,

	@Schema(example = "2.5")
	double percentage
) {

	public static PackBonusResponse from(PackBonusRule rule) {
		return new PackBonusResponse(RaritySummaryResponse.from(rule.getRarity()), rule.getProbabilityUnits(),
			rule.getProbabilityUnits() * 100.0 / PackBonusRule.UNITS);
	}
}
