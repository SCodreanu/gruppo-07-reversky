package it.downfall.reversky.engine.core.effect.impl;

/**
 * Dissoluzione (Dissolution), Spell {@code DAMAGE}: "avv. −2 ATK, −2 DEF".
 */
public final class DissolutionEffect extends DamageSpellEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "DISSOLUTION";

	/** Crea l'effetto. */
	public DissolutionEffect() {
		super(2, 2);
	}

	@Override
	public String code() {
		return CODE;
	}
}
