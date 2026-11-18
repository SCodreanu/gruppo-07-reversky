package it.downfall.reversky.engine.core.effect.impl;

/**
 * Morso del Rovescio (Bite of the Reverse), Spell {@code DAMAGE}: "avv. −3 ATK".
 */
public final class BiteOfReverseEffect extends DamageSpellEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "BITE_OF_REVERSE";

	/** Crea l'effetto. */
	public BiteOfReverseEffect() {
		super(3, 0);
	}

	@Override
	public String code() {
		return CODE;
	}
}
