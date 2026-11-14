package it.downfall.reversky.api.ruleset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Modifica di una versione mai usata (PUT /admin/rule-sets/{id}, docs/backoffice).
 * <p>
 * Come {@link RuleSetCreateRequest}: la versione può cambiare finché il rule set non è
 * stato usato da una partita (a quel punto {@code RuleSetInUseException} → 409).
 */
public record RuleSetUpdateRequest(

	@Schema(example = "1.1")
	@NotBlank(message = "Version is required")
	@Size(max = 20, message = "Version must not exceed 20 characters")
	String version,

	@Schema(example = "20")
	@NotNull(message = "Starting life points are required")
	@Min(value = 1, message = "Starting life points must be at least 1")
	Integer startingLifePoints,

	@Schema(example = "10")
	@NotNull(message = "Max rounds are required")
	@Min(value = 1, message = "Max rounds must be at least 1")
	Integer maxRounds,

	@Schema(example = "30")
	@NotNull(message = "Deck size is required")
	@Min(value = 1, message = "Deck size must be at least 1")
	Integer deckSize,

	@Schema(example = "15")
	@NotNull(message = "Deck creatures are required")
	@Min(value = 1, message = "Deck creatures must be at least 1")
	Integer deckCreatures,

	@Schema(example = "5")
	@NotNull(message = "Deck spells are required")
	@Min(value = 0, message = "Deck spells must not be negative")
	Integer deckSpells,

	@Schema(example = "4")
	@NotNull(message = "Deck hidden are required")
	@Min(value = 0, message = "Deck hidden must not be negative")
	Integer deckHidden,

	@Schema(example = "3")
	@NotNull(message = "Deck items are required")
	@Min(value = 0, message = "Deck items must not be negative")
	Integer deckItems,

	@Schema(example = "3")
	@NotNull(message = "Deck terrains are required")
	@Min(value = 0, message = "Deck terrains must not be negative")
	Integer deckTerrains,

	@Schema(example = "2")
	@NotNull(message = "Max hidden survival is required")
	@Min(value = 0, message = "Max hidden survival must not be negative")
	Integer maxHiddenSurvival,

	@Schema(example = "3")
	@NotNull(message = "Max hidden damage is required")
	@Min(value = 0, message = "Max hidden damage must not be negative")
	Integer maxHiddenDamage,

	@Schema(example = "100")
	@NotNull(message = "Signup bonus is required")
	@Min(value = 0, message = "Signup bonus must not be negative")
	Integer signupBonus,

	@Schema(example = "50")
	@NotNull(message = "Win reward is required")
	@Min(value = 0, message = "Win reward must not be negative")
	Integer winReward,

	@Schema(example = "20")
	@NotNull(message = "Draw reward is required")
	@Min(value = 0, message = "Draw reward must not be negative")
	Integer drawReward
) {
}