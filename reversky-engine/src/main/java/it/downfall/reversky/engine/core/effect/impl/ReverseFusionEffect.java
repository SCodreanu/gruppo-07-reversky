package it.downfall.reversky.engine.core.effect.impl;

/**
 * Fusione del Rovescio (Reverse Fusion), Spell {@code BURN}: "se la carta rivelata è una creatura, entra con +2 ATK e
 * +1 DEF; altrimenti la prossima creatura ottiene comunque +1 ATK".
 */
public final class ReverseFusionEffect extends BurnSpellEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "REVERSE_FUSION";

	/** Crea l'effetto. */
	public ReverseFusionEffect() {
		super(2, 1, 1, 0);
	}

	@Override
	public String code() {
		return CODE;
	}
}
