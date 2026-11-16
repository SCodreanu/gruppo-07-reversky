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

	Instant createdAt,

	@Schema(example = "1", nullable = true)
	Integer createdById
) {

	/**
	 * Converte l'entità nel DTO.
	 */
	public static RuleSetResponse from(RuleSet ruleSet) {
		return new RuleSetResponse(
			ruleSet.getId(),
			ruleSet.getVersion(),
			ruleSet.isActive(),
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
