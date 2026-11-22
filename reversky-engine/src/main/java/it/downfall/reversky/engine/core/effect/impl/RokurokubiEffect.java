package it.downfall.reversky.engine.core.effect.impl;

/**
 * Rokurokubi, Uncommon (Theory of the Dream): "all'entrata avv. −1 DEF e −1 DEF alla prossima creatura
 * avversaria che entra".
 */
public final class RokurokubiEffect extends EntryAndNextDamageEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "ROKUROKUBI";

	/** Crea l'effetto. */
	public RokurokubiEffect() {
		super(1);
	}

	@Override
	public String code() {
		return CODE;
	}
}
