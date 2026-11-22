package it.downfall.reversky.engine.core.model;

import java.util.List;

import it.downfall.reversky.engine.core.rules.FactionBonus;
import it.downfall.reversky.engine.core.rules.RuleConfig;

/**
 * Tutto ciò che serve per simulare una partita: è il corpo di {@code POST /api/v1/simulations} convertito
 * nel core (docs/api-engine). L'engine non ha valori predefiniti: regole, fazioni e bonus arrivano qui.
 *
 * @param seed           seme del mescolamento, generato dall'API
 * @param rules          regole del rule set attivo
 * @param factions       fazioni della tabella {@code factions}; copia immutabile
 * @param factionBonuses righe della tabella {@code faction_bonuses}, anche vuota; copia immutabile
 * @param challenger     sfidante
 * @param opponent       avversario
 */
public record SimulationInput(
		long seed,
		RuleConfig rules,
		List<Faction> factions,
		List<FactionBonus> factionBonuses,
		PlayerInput challenger,
		PlayerInput opponent) {

	/**
	 * Copia le liste (CodingRequirements §7). Una lista nulla è un errore di programmazione:
	 * {@code List.copyOf} lancia {@link NullPointerException}.
	 */
	public SimulationInput {
		factions = List.copyOf(factions);
		factionBonuses = List.copyOf(factionBonuses);
	}
}
