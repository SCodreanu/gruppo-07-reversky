package it.downfall.reversky.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.entity.FactionBonus;

/**
 * Riga di {@code faction_bonuses} per il backoffice (GET/PUT /admin/faction-bonuses):
 * include i nomi delle fazioni per non costringere il pannello admin a un join client-side.
 */
public record FactionBonusResponse(

	@Schema(example = "1")
	Integer originFactionId,

	@Schema(example = "Theory of the Wound")
	String originFactionName,

	@Schema(example = "2")
	Integer targetFactionId,

	@Schema(example = "Theory of the Dream")
	String targetFactionName,

	@Schema(example = "2")
	Integer atkBonus,

	@Schema(example = "1")
	Integer defBonus
) {

	public static FactionBonusResponse from(FactionBonus bonus, String originFactionName, String targetFactionName) {
		return new FactionBonusResponse(
			bonus.getOriginFactionId(),
			originFactionName,
			bonus.getTargetFactionId(),
			targetFactionName,
			bonus.getAtkBonus(),
			bonus.getDefBonus());
	}
}
