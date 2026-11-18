package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.TerrainEffect;

/**
 * Base dei Terreni con il solo bonus base (D-17, D-18): ATK e DEF in più a tutte le creature in campo della fazione
 * del Terreno, di entrambi i giocatori. Gli effetti secondari arrivano con ENG-27.
 */
public abstract class BaseTerrainEffect implements TerrainEffect {

	private final int attackBonus;
	private final int defenseBonus;

	/**
	 * Crea la base.
	 *
	 * @param attackBonus  ATK in più, almeno 0
	 * @param defenseBonus DEF in più, almeno 0
	 */
	protected BaseTerrainEffect(int attackBonus, int defenseBonus) {
		this.attackBonus = attackBonus;
		this.defenseBonus = defenseBonus;
	}

	@Override
	public final int attackBonus() {
		return attackBonus;
	}

	@Override
	public final int defenseBonus() {
		return defenseBonus;
	}
}
