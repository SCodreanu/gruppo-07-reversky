package it.downfall.reversky.engine.core.effect;

import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PlacedTerrain;
import it.downfall.reversky.engine.core.model.Wound;

/**
 * Effetto di un Terreno: un bonus base passivo a tutte le creature in campo della fazione del Terreno, di entrambi i
 * giocatori (D-17), finché il Terreno resta in gioco (D-19).
 * <p>
 * Il motore applica il bonus (ENG-21); l'effetto dice solo quanto vale. Gli effetti secondari (ENG-27, D-18) usano i
 * momenti qui sotto, che il motore chiama solo per le creature della fazione del Terreno
 * ({@link PlacedTerrain#affects}), di entrambi i giocatori. L'istanza è una per messa in gioco, quindi può tenere lo
 * stato "una volta per scontro".
 */
public non-sealed interface TerrainEffect extends CardEffect {

	/**
	 * Bonus ATK base.
	 *
	 * @return punti di ATK in più, almeno 0
	 */
	int attackBonus();

	/**
	 * Bonus DEF base.
	 *
	 * @return punti di DEF in più, almeno 0
	 */
	int defenseBonus();

	/**
	 * Inizio di uno scontro, alla fine del passo 0 (Devouring Abyss): una volta per ogni creatura viva della fazione
	 * del Terreno, prima lo sfidante; dopo ognuna si controllano le morti.
	 *
	 * @param state    partita
	 * @param terrain  il Terreno in gioco
	 * @param creature creatura della fazione del Terreno
	 */
	default void onCombatStart(MatchState state, PlacedTerrain terrain, CreatureInPlay creature) {
	}

	/**
	 * Una creatura della fazione del Terreno è stata ferita (Echo Rift): al controllo della DEF, dopo il suo
	 * {@code onWounded}, solo se è ancora viva.
	 *
	 * @param state    partita
	 * @param terrain  il Terreno in gioco
	 * @param creature creatura ferita
	 * @param wound    la ferita
	 */
	default void onWounded(MatchState state, PlacedTerrain terrain, CreatureInPlay creature, Wound wound) {
	}

	/**
	 * Riduce una penalità DEF che l'avversario infligge a una creatura della fazione del Terreno (Hollow Clearing).
	 *
	 * @param state   partita
	 * @param terrain il Terreno in gioco
	 * @param target  creatura colpita
	 * @param amount  danno, almeno 0
	 * @return il danno ridotto, almeno 0; di base invariato
	 */
	default int reduceDefensePenalty(MatchState state, PlacedTerrain terrain, CreatureInPlay target, int amount) {
		return amount;
	}

	/**
	 * Una Spell {@code DAMAGE} dell'avversario sta per colpire una creatura della fazione del Terreno (Watching Rift).
	 *
	 * @param state   partita
	 * @param terrain il Terreno in gioco
	 * @param target  creatura colpita
	 * @param spell   nome della Spell, per il log
	 * @return i punti da togliere al colpo, prima dalla DEF e poi dall'ATK; di base 0
	 */
	default int reduceDamageSpell(MatchState state, PlacedTerrain terrain, CreatureInPlay target, String spell) {
		return 0;
	}
}
