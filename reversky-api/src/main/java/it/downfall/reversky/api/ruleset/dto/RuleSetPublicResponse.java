package it.downfall.reversky.api.ruleset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.ruleset.entity.RuleSet;

/**
 * Regole attive per GET /rules (pubblico): niente id, niente {@code createdBy}, niente
 * {@code isActive}, perché c'è solo quella attiva.
 */
public record RuleSetPublicResponse(

	@Schema(example = "1.0")
	String version,

	@Schema(example = "20")
	Integer startingLifePoints,

	@Schema(example = "10")
	Integer maxRounds,

	@Schema(example = "30")
	Integer deckSize,

	@Schema(example = "15")
	Integer deckCreatures,

	@Schema(example = "5")
	Integer deckSpells,

	@Schema(example = "4")
	Integer deckHidden,

	@Schema(example = "3")
	Integer deckItems,

	@Schema(example = "3")
	Integer deckTerrains,

	@Schema(example = "2")
	Integer maxHiddenSurvival,

	@Schema(example = "3")
	Integer maxHiddenDamage,

	@Schema(example = "100")
	Integer signupBonus,

	@Schema(example = "50")
	Integer winReward,

	@Schema(example = "20")
	Integer drawReward
) {

	public static RuleSetPublicResponse from(RuleSet ruleSet) {
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
			ruleSet.getDrawReward());
	}
}