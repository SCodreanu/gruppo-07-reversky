package it.downfall.reversky.api.battle.client;

import java.util.List;

/**
 * Richiesta di {@code POST /api/v1/simulations} dell'engine (reversky-engine/docs/api-engine). I nomi dei campi sono
 * il contratto: non cambiarli senza cambiare anche l'engine.
 *
 * @param seed           generato dall'API con {@code SecureRandom}
 * @param factionBonuses anche vuota
 */
public record SimulationRequest(
	long seed,
	Rules rules,
	List<Faction> factions,
	List<FactionBonus> factionBonuses,
	Player challenger,
	Player opponent
) {

	/** Regole della partita, dal rule set copiato alla preparazione. */
	public record Rules(String rulesVersion, int startingLifePoints, int maxRounds) {
	}

	public record Faction(Integer id, String name) {
	}

	public record FactionBonus(Integer originFactionId, Integer targetFactionId, int atkBonus, int defBonus) {
	}

	/**
	 * @param deck una voce per ogni copia di ogni carta
	 */
	public record Player(Integer userId, List<Card> deck) {
	}

	/**
	 * Una copia di una carta, con i valori del catalogo al momento della preparazione.
	 */
	public record Card(
		Integer cardId,
		String name,
		String type,
		String rarity,
		int rarityOrder,
		Integer factionId,
		Integer secondaryFactionId,
		Integer atk,
		Integer def,
		String subtype,
		String effectCode
	) {
	}
}
