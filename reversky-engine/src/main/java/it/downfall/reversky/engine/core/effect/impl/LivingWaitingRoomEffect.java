package it.downfall.reversky.engine.core.effect.impl;

/**
 * Sala d'Attesa Vivente (Living Waiting Room), Uncommon (Theory of the Wound): "previene 2 danni dal primo
 * attacco di ogni creatura avversaria".
 */
public final class LivingWaitingRoomEffect extends FirstAttackShieldEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "LIVING_WAITING_ROOM";

	/** Crea l'effetto. */
	public LivingWaitingRoomEffect() {
		super(2);
	}

	@Override
	public String code() {
		return CODE;
	}
}
