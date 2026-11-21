package it.downfall.reversky.engine.core.effect.impl;

/**
 * Survival Instinct, Hidden {@code SURVIVAL}: "+2 ATK, +1 DEF nello scontro corrente".
 */
public final class SurvivalInstinctEffect extends SurvivalHiddenEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SURVIVAL_INSTINCT";

	/** Crea l'effetto. */
	public SurvivalInstinctEffect() {
		super(2, 1);
	}

	@Override
	public String code() {
		return CODE;
	}
}
