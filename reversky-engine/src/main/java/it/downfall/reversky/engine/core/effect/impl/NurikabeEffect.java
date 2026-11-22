package it.downfall.reversky.engine.core.effect.impl;

/**
 * Nurikabe, Uncommon (Theory of the Dream): "previene 2 danni dal primo attacco di ogni creatura avversaria".
 */
public final class NurikabeEffect extends FirstAttackShieldEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "NURIKABE";

	/** Crea l'effetto. */
	public NurikabeEffect() {
		super(2);
	}

	@Override
	public String code() {
		return CODE;
	}
}
