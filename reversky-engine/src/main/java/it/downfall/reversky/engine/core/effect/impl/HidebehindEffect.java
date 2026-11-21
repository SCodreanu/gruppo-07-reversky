package it.downfall.reversky.engine.core.effect.impl;

/**
 * Hidebehind, Uncommon (Theory of the Invitation): "previene 2 danni dal primo attacco di ogni creatura avversaria".
 */
public final class HidebehindEffect extends FirstAttackShieldEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "HIDEBEHIND";

	/** Crea l'effetto. */
	public HidebehindEffect() {
		super(2);
	}

	@Override
	public String code() {
		return CODE;
	}
}
