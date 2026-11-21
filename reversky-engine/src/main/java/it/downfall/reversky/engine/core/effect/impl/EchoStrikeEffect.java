package it.downfall.reversky.engine.core.effect.impl;

/**
 * Echo Strike, Hidden {@code DAMAGE}: "avv. −2 ATK nel primo round e −2 DEF".
 */
public final class EchoStrikeEffect extends FirstRoundHiddenEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "ECHO_STRIKE";

	/** Crea l'effetto. */
	public EchoStrikeEffect() {
		super(2, 2);
	}

	@Override
	public String code() {
		return CODE;
	}
}
