package it.downfall.reversky.engine.core.effect.impl;

/**
 * The Gnawing Witness, Uncommon (Theory of the Wound): "all'entrata 2 danni all'avv.".
 */
public final class GnawingWitnessEffect extends EntryDamageEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "GNAWING_WITNESS";

	/** Crea l'effetto. */
	public GnawingWitnessEffect() {
		super(2);
	}

	@Override
	public String code() {
		return CODE;
	}
}
