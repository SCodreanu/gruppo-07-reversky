package it.downfall.reversky.api.ruleset.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.factionbonus.dto.FactionBonusSummaryResponse;
import it.downfall.reversky.api.ruleset.entity.RuleSet;

/**
 * Regole attive per GET /rules (pubblico): niente id, niente {@code createdBy}, niente
 * {@code isActive}, perché c'è solo quella attiva.
 * <p>
 * {@code factionBonuses} è la matrice corrente: così client ed engine hanno un solo punto da
 * leggere e il rule set resta autoconsistente (bonus e regole viaggiano insieme).
 */
public record RuleSetPublicResponse(

	@Schema(example = "1.0")
	String version,

	@Schema(example = "30")
	Integer startingLifePoints,

	@Schema(example = "75")
	Integer maxRounds,

	@Schema(example = "30")
	Integer deckSize,

	@Schema(example = "20")
	Integer deckCreatures,

	@Schema(example = "6")
	Integer deckSpells,

	@Schema(example = "2")
	Integer deckHidden,

	@Schema(example = "1")
	Integer deckItems,

	@Schema(example = "1")
	Integer deckTerrains,

	@Schema(example = "2")
	Integer maxHiddenSurvival,

	@Schema(example = "1")
	Integer maxHiddenDamage,

	@Schema(example = "500")
	Integer signupBonus,

	@Schema(example = "30")
	Integer winReward,

	@Schema(example = "10")
	Integer drawReward,

	List<FactionBonusSummaryResponse> factionBonuses
) {

	/**
	 * Converte l'entità nel DTO, allega la matrice dei bonus passata dal service.
	 */
	public static RuleSetPublicResponse from(RuleSet ruleSet, List<FactionBonusSummaryResponse> factionBonuses) {
		return new RuleSetPublicResponse(
			ruleSet.getVersion(),
			ruleSet.getStartingLifePoints(),
			ruleSet.getMaxRounds(),
			ruleSet.getDeckSize(),
			ruleSet.getDeckCreatures(),
			ruleSet.getDeckSpells(),
			ruleSet.getDeckHidden(),
			ruleSet.getDeckItems(),
			ruleSet.getDeckTerrains(),
			ruleSet.getMaxHiddenSurvival(),
			ruleSet.getMaxHiddenDamage(),
			ruleSet.getSignupBonus(),
			ruleSet.getWinReward(),
			ruleSet.getDrawReward(),
			factionBonuses);
	}
}