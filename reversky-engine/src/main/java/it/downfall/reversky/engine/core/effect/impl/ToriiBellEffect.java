package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.ItemEffect;

/**
 * Campanello del Torii, Oggetto della Theory of the Dream, <i>Campana d'allarme</i>: "quando la prima Hidden nemica
 * starebbe per attivarsi, viene annullata (scartata senza effetto)".
 */
public final class ToriiBellEffect implements ItemEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "TORII_BELL";

	/** Crea l'effetto. */
	public ToriiBellEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public boolean cancelsEnemyHidden() {
		return true;
	}
}
