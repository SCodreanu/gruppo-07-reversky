package it.downfall.reversky.api.ruleset.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.ruleset.entity.RuleSet;

/**
 * Rule set completo per POST/PUT e per la risposta del backoffice.
 */
public record RuleSetResponse(

	@Schema(example = "1")
	Integer id,

	@Schema(example = "1.0")
	String version,

	@Schema(example = "true")
	Boolean isActive,

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
	Integer drawReward,

	Instant createdAt,

	@Schema(example = "1", nullable = true)
	Integer createdById
) {

	public static RuleSetResponse from(RuleSet ruleSet) {
		return new RuleSetResponse(
			ruleSet.getId(),
			ruleSet.getVersion(),
			ruleSet.getIsActive(),
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
			ruleSet.getCreatedAt(),
			ruleSet.getCreatedById());
	}
}