package it.downfall.reversky.api.battle.entity;

import java.util.List;

import it.downfall.reversky.api.battle.client.SimulationRequest;

/**
 * Contenuto di {@code battles.battle_stats} (docs/sfide, "Contenuto di battle_stats").
 * <p>
 * Seed, regole, fazioni e bonus si scrivono alla preparazione e servono al retry, che rimanda all'engine esattamente
 * la stessa partita; motivo di fine, round e statistiche dei giocatori arrivano dall'engine e restano null finché la
 * partita non è completata.
 */
public record BattleStats(
	long seed,
	SimulationRequest.Rules rules,
	List<SimulationRequest.Faction> factions,
	List<SimulationRequest.FactionBonus> factionBonuses,
	String endReason,
	Integer totalRounds,
	PlayerStats challenger,
	PlayerStats opponent
) {

	/** Statistiche alla preparazione, senza esito. */
	public static BattleStats prepared(long seed, SimulationRequest.Rules rules, List<SimulationRequest.Faction> factions,
			List<SimulationRequest.FactionBonus> factionBonuses) {
		return new BattleStats(seed, rules, factions, factionBonuses, null, null, null, null);
	}

	/** Le stesse statistiche con l'esito dell'engine. */
	public BattleStats completed(String endReason, int totalRounds, PlayerStats challenger, PlayerStats opponent) {
		return new BattleStats(seed, rules, factions, factionBonuses, endReason, totalRounds, challenger, opponent);
	}

	/**
	 * @param finalLifePoints può essere negativo (D-43)
	 */
	public record PlayerStats(int finalLifePoints, int creaturesDefeated) {
	}
}
