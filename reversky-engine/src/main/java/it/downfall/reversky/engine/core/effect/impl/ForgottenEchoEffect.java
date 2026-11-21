package it.downfall.reversky.engine.core.effect.impl;

/**
 * Eco dei Dimenticati (Echo of the Forgotten), Spell {@code BURN}: "se la carta rivelata è una creatura, entra con +2
 * ATK e +1 DEF; altrimenti la prossima creatura ottiene comunque +2 DEF".
 */
public final class ForgottenEchoEffect extends BurnSpellEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "FORGOTTEN_ECHO";

	/** Crea l'effetto. */
	public ForgottenEchoEffect() {
		super(2, 1, 0, 2);
	}

	@Override
	public String code() {
		return CODE;
	}
}
