package it.downfall.reversky.engine.core.effect.impl;

/**
 * SCP-049 (Plague Doctor), Uncommon (Theory of the Archive): "all'entrata avv. −2 ATK nel primo round e −1 nel
 * secondo".
 */
public final class Scp049Effect extends FadingAttackPenaltyEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SCP_049";

	/** Crea l'effetto. */
	public Scp049Effect() {
	}

	@Override
	public String code() {
		return CODE;
	}
}
