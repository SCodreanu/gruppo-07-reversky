package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PlacedTerrain;

/**
 * Hollow Clearing (Crepa Radura), Terreno della Theory of the Invitation: bonus base +2 DEF. Effetto secondario,
 * <i>Ogni penalità</i> (ENG-27): ogni penalità DEF che ricevono le creature della fazione è ridotta di 1.
 * <p>
 * Penalità = DEF persa per un effetto, una Spell, una Hidden, un modificatore in attesa o un Terreno dell'avversario,
 * non per un attacco né per un sacrificio della creatura stessa (deciso il 18 novembre). Vale per la fazione, anche
 * prima che la creatura riceva il bonus (un danno in attesa all'entrata). Breaking Point toglie una DEF in meno, quindi
 * la creatura resta a 1 (deciso il 18 novembre).
 */
public final class HollowClearingEffect extends BaseTerrainEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "HOLLOW_CLEARING";

	private static final int REDUCTION = 1;

	/** Crea l'effetto. */
	public HollowClearingEffect() {
		super(0, 2);
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public int reduceDefensePenalty(MatchState state, PlacedTerrain terrain, CreatureInPlay target, int amount) {
		if (amount <= 0) {
			return amount;
		}
		int reduced = Math.max(0, amount - REDUCTION);
		state.log().effectTriggered(target.side(), target.name(),
				"takes " + (amount - reduced) + " less damage (" + terrain.card().name() + ")");
		return reduced;
	}
}
