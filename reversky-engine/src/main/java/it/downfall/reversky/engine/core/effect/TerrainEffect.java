package it.downfall.reversky.engine.core.effect;

/**
 * Effetto di un Terreno: un bonus base passivo a tutte le creature in campo della fazione del Terreno, di entrambi i
 * giocatori (D-17), finché il Terreno resta in gioco (D-19).
 * <p>
 * Il motore applica il bonus (ENG-21); l'effetto dice solo quanto vale. Gli effetti secondari arrivano con ENG-27.
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
}
