package it.downfall.reversky.engine.core.effect.impl;

/**
 * Noppera-bō, Uncommon (Theory of the Dream): "all'entrata avv. −2 ATK nel primo round e −1 nel secondo".
 */
public final class NopperaBoEffect extends FadingAttackPenaltyEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "NOPPERA_BO";

	@Override
	public String code() {
		return CODE;
	}
}
