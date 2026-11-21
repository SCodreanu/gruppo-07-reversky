package it.downfall.reversky.engine.core.effect.impl;

/**
 * Last Sanctuary, Hidden {@code SURVIVAL}: "propria creatura +5 DEF nello scontro corrente".
 */
public final class LastSanctuaryEffect extends SurvivalHiddenEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "LAST_SANCTUARY";

	/** Crea l'effetto. */
	public LastSanctuaryEffect() {
		super(0, 5);
	}

	@Override
	public String code() {
		return CODE;
	}
}
