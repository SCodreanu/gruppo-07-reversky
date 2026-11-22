package it.downfall.reversky.engine.core.effect.impl;

/**
 * Cartellino Sbiadito (Faded Tag), Uncommon (Theory of the Wound): "all'entrata avv. −1 DEF e −1 DEF alla
 * prossima creatura avversaria che entra".
 */
public final class FadedTagEffect extends EntryAndNextDamageEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "FADED_TAG";

	/** Crea l'effetto. */
	public FadedTagEffect() {
		super(1);
	}

	@Override
	public String code() {
		return CODE;
	}
}
