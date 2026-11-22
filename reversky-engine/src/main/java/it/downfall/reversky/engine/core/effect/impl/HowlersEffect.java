package it.downfall.reversky.engine.core.effect.impl;

/**
 * Howlers, Uncommon (Theory of the Wound): "all'entrata avv. −2 ATK nel primo round e −1 nel secondo".
 */
public final class HowlersEffect extends FadingAttackPenaltyEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "HOWLERS";

	/** Crea l'effetto. */
	public HowlersEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}
}
