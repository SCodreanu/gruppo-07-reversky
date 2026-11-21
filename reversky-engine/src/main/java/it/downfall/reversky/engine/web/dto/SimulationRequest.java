package it.downfall.reversky.engine.web.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo di {@code POST /api/v1/simulations} (docs/api-engine).
 * <p>
 * Qui si controlla solo la forma (controllo 1, VALIDATION_FAILED): campi obbligatori, valori minimi delle regole,
 * mazzi non vuoti. La coerenza dei dati (fazioni, bonus, carte) la controlla il core.
 *
 * @param seed           seed della partita, generato dall'API
 * @param rules          regole del rule set attivo
 * @param factions       fazioni della tabella {@code factions}
 * @param factionBonuses righe di {@code faction_bonuses}, anche nessuna
 * @param challenger     sfidante
 * @param opponent       avversario
 */
public record SimulationRequest(

		@NotNull(message = "Seed is required")
		Long seed,

		@NotNull(message = "Rules are required")
		@Valid
		RulesRequest rules,

		@NotNull(message = "Factions are required")
		List<@NotNull(message = "Faction must not be null") @Valid FactionRequest> factions,

		@NotNull(message = "Faction bonuses are required (use an empty list for none)")
		List<@NotNull(message = "Faction bonus must not be null") @Valid FactionBonusRequest> factionBonuses,

		@NotNull(message = "Challenger is required")
		@Valid
		PlayerRequest challenger,

		@NotNull(message = "Opponent is required")
		@Valid
		PlayerRequest opponent) {
}
