package it.downfall.reversky.engine.core.effect.impl;

/**
 * Hostile Presence, Hidden {@code DAMAGE}: "avv. −5 ATK nel primo round dello scontro".
 */
public final class HostilePresenceEffect extends FirstRoundHiddenEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "HOSTILE_PRESENCE";

	/** Crea l'effetto. */
	public HostilePresenceEffect() {
		super(5, 0);
	}

	@Override
	public String code() {
		return CODE;
	}
}
