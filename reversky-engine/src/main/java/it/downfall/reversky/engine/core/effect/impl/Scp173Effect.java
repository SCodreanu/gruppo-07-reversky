package it.downfall.reversky.engine.core.effect.impl;

/**
 * SCP-173 (The Sculpture), Uncommon (Theory of the Archive): "all'entrata 2 danni all'avv.".
 */
public final class Scp173Effect extends EntryDamageEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SCP_173";

	/** Crea l'effetto. */
	public Scp173Effect() {
		super(2);
	}

	@Override
	public String code() {
		return CODE;
	}
}
