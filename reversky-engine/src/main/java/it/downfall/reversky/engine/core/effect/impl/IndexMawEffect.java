package it.downfall.reversky.engine.core.effect.impl;

/**
 * The Index Maw, Uncommon (Theory of the Archive): "all'entrata +2 DEF finché resta in campo".
 */
public final class IndexMawEffect extends EntryDefenseEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "INDEX_MAW";

	/** Crea l'effetto. */
	public IndexMawEffect() {
		super(2);
	}

	@Override
	public String code() {
		return CODE;
	}
}
