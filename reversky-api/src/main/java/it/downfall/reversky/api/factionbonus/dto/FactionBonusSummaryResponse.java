package it.downfall.reversky.api.factionbonus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.factionbonus.entity.FactionBonus;

/**
 * Riga di {@code faction_bonuses} per GET /rules: solo i valori, senza nomi,
 * perché l'engine e i client ricevono solo quello che serve al gioco.
 */
public record FactionBonusSummaryResponse(

	@Schema(example = "1")
	Integer originFactionId,

	@Schema(example = "2")
	Integer targetFactionId,

	@Schema(example = "2")
	Integer atkBonus,

	@Schema(example = "1")
	Integer defBonus
) {

	public static FactionBonusSummaryResponse from(FactionBonus bonus) {
		return new FactionBonusSummaryResponse(
			bonus.getOriginFactionId(),
			bonus.getTargetFactionId(),
			bonus.getAtkBonus(),
			bonus.getDefBonus());
	}
}