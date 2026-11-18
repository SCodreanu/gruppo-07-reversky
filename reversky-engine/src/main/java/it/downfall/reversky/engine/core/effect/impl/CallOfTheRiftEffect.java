package it.downfall.reversky.engine.core.effect.impl;

/**
 * Richiamo della Crepa (Call of the Rift), Spell {@code BURN}: "se la carta rivelata è una creatura, entra con +2 ATK
 * e +2 DEF; altrimenti nulla".
 */
public final class CallOfTheRiftEffect extends BurnSpellEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "CRACK_CALL";

	/** Crea l'effetto. */
	public CallOfTheRiftEffect() {
		super(2, 2, 0, 0);
	}

	@Override
	public String code() {
		return CODE;
	}
}
