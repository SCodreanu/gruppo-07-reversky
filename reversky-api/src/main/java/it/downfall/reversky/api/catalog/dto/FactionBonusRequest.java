package it.downfall.reversky.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Singola riga della matrice di bonus inviata con PUT /admin/faction-bonuses
 * (docs/backoffice, "Dati di riferimento").
 * <p>
 * {@code atkBonus} può essere negativo (malus, es. -1). {@code defBonus} deve essere ≥ 0.
 * Le fazioni devono esistere e non possono essere uguali (controlli nel service).
 */
public record FactionBonusRequest(

	@Schema(example = "1")
	@NotNull(message = "Origin faction id is required")
	Integer originFactionId,

	@Schema(example = "2")
	@NotNull(message = "Target faction id is required")
	Integer targetFactionId,

	@Schema(example = "2", description = "Can be negative (malus)")
	@NotNull(message = "ATK bonus is required")
	Integer atkBonus,

	@Schema(example = "1", description = "Must not be negative")
	@NotNull(message = "DEF bonus is required")
	@Min(value = 0, message = "DEF bonus must not be negative")
	Integer defBonus
) {
}
