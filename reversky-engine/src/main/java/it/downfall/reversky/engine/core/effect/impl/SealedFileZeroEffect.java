package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.ItemEffect;

/**
 * Fascicolo Sigillato 0, Oggetto della Theory of the Archive, <i>Censurato</i>: "il primo effetto di creatura, Spell o
 * Hidden avversaria che colpirebbe la creatura del proprietario viene annullato del tutto (non gli attacchi, non i
 * Terreni)".
 */
public final class SealedFileZeroEffect implements ItemEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SEALED_FILE_ZERO";

	/** Crea l'effetto. */
	public SealedFileZeroEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public boolean cancelsEnemyEffect() {
		return true;
	}
}
