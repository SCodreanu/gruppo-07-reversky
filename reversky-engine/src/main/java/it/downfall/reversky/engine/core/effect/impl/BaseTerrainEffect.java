package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.TerrainEffect;

/**
 * Base dei Terreni (D-17, D-18): il bonus base, ATK e DEF in più a tutte le creature in campo della fazione del
 * Terreno, di entrambi i giocatori. Ogni Terreno aggiunge il suo effetto secondario con i momenti di
 * {@code TerrainEffect} (ENG-27).
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
