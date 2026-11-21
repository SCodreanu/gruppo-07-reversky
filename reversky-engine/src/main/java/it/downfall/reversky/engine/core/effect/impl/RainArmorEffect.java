package it.downfall.reversky.engine.core.effect.impl;

/**
 * Armatura della Pioggia (Rain Armor), Spell {@code SHIELD}: "+2 ATK".
 */
public final class RainArmorEffect extends ShieldSpellEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "RAIN_ARMOR";

	/** Crea l'effetto. */
	public RainArmorEffect() {
		super(2, 0);
	}

	@Override
	public String code() {
		return CODE;
	}
}
