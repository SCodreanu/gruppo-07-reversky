package it.downfall.reversky.engine.core.effect.impl;

/**
 * Velo degli Ancorati (Veil of the Anchored), Spell {@code SHIELD}: "+5 DEF".
 */
public final class VeilOfAnchoredEffect extends ShieldSpellEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "VEIL_OF_ANCHORED";

	/** Crea l'effetto. */
	public VeilOfAnchoredEffect() {
		super(0, 5);
	}

	@Override
	public String code() {
		return CODE;
	}
}
