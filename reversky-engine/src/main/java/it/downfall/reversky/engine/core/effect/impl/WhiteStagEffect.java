package it.downfall.reversky.engine.core.effect.impl;

/**
 * Cervo Bianco (White Stag), Uncommon (Theory of the Invitation): "all'entrata avv. −1 DEF e −1 DEF alla prossima
 * creatura avversaria che entra".
 */
public final class WhiteStagEffect extends EntryAndNextDamageEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "WHITE_STAG";

	/** Crea l'effetto. */
	public WhiteStagEffect() {
		super(1);
	}

	@Override
	public String code() {
		return CODE;
	}
}
