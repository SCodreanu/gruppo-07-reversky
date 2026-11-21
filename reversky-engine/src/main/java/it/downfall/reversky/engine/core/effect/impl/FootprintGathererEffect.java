package it.downfall.reversky.engine.core.effect.impl;

/**
 * Il Raccoglitore di Orme, Uncommon (Theory of the Invitation): "all'entrata +2 DEF finché resta in campo".
 */
public final class FootprintGathererEffect extends EntryDefenseEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "FOOTPRINT_GATHERER";

	/** Crea l'effetto. */
	public FootprintGathererEffect() {
		super(2);
	}

	@Override
	public String code() {
		return CODE;
	}
}
