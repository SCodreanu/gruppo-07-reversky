package it.downfall.reversky.engine.core.effect.impl;

/**
 * Hollow Clearing (Crepa Radura), Terreno della Theory of the Invitation: bonus base +2 DEF. Effetto secondario
 * (ENG-27): ogni penalità DEF ricevuta è ridotta di 1.
 */
public final class HollowClearingEffect extends BaseTerrainEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "HOLLOW_CLEARING";

	/** Crea l'effetto. */
	public HollowClearingEffect() {
		super(0, 2);
	}

	@Override
	public String code() {
		return CODE;
	}
}
