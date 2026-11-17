package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.ItemEffect;

/**
 * Bussola dei Sentieri Falsi, Oggetto della Theory of the Invitation, <i>Strada lunga</i>: "all'inizio del prossimo
 * scontro, uno scudo di 2 DEF all'inizio di ognuno dei primi tre round (si rinnova, non si somma)". Se lo scontro
 * finisce prima, i round restanti si perdono.
 */
public final class FalsePathsCompassEffect implements ItemEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "FALSE_PATHS_COMPASS";

	private static final int SHIELD = 2;
	private static final int ROUNDS = 3;

	/** Crea l'effetto. */
	public FalsePathsCompassEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public int roundStartShield() {
		return SHIELD;
	}

	@Override
	public int roundStartShieldRounds() {
		return ROUNDS;
	}
}
