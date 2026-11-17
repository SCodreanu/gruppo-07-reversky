package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.ItemEffect;

/**
 * Badge di Uscita Negata, Oggetto della Theory of the Wound, <i>Nessuna uscita</i>: "la prima volta che la creatura
 * del proprietario morirebbe, per qualunque danno, resta con 1 DEF".
 */
public final class DeniedExitBadgeEffect implements ItemEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "DENIED_EXIT_BADGE";

	private static final int SURVIVAL_DEFENSE = 1;

	/** Crea l'effetto. */
	public DeniedExitBadgeEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public int survivalDefense() {
		return SURVIVAL_DEFENSE;
	}
}
