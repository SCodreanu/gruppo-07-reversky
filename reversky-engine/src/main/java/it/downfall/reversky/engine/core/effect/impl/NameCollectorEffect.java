package it.downfall.reversky.engine.core.effect.impl;

/**
 * Il Collezionista di Nomi, Uncommon (Theory of the Archive): "all'entrata avv. −1 DEF e −1 DEF alla prossima
 * creatura avversaria che entra".
 */
public final class NameCollectorEffect extends EntryAndNextDamageEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "NAME_COLLECTOR";

	/** Crea l'effetto. */
	public NameCollectorEffect() {
		super(1);
	}

	@Override
	public String code() {
		return CODE;
	}
}
