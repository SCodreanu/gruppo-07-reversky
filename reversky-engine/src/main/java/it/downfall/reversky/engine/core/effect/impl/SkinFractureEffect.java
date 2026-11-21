package it.downfall.reversky.engine.core.effect.impl;

/**
 * Frattura della Pelle (Skin Fracture), Spell {@code DAMAGE}: "avv. −5 DEF".
 */
public final class SkinFractureEffect extends DamageSpellEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SKIN_FRACTURE";

	/** Crea l'effetto. */
	public SkinFractureEffect() {
		super(0, 5);
	}

	@Override
	public String code() {
		return CODE;
	}
}
