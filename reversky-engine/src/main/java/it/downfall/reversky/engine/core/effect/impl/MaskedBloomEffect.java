package it.downfall.reversky.engine.core.effect.impl;

/**
 * The Masked Bloom, Uncommon (Theory of the Dream): "all'entrata avv. −2 ATK nel primo round e −1 nel
 * secondo".
 */
public final class MaskedBloomEffect extends FadingAttackPenaltyEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "MASKED_BLOOM";

	/** Crea l'effetto. */
	public MaskedBloomEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}
}
