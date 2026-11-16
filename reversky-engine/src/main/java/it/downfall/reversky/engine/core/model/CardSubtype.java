package it.downfall.reversky.engine.core.model;

/**
 * Sottotipo di Spell e Hidden ({@code catalog_cards.subtype}); le altre carte non ne hanno.
 */
public enum CardSubtype {

	/** Spell o Hidden che colpisce la creatura avversaria. */
	DAMAGE,

	/** Spell che protegge o rafforza la prossima creatura del proprietario. */
	SHIELD,

	/** Spell che rivela la prossima carta del mazzo del proprietario (D-13). */
	BURN,

	/** Hidden che protegge la creatura del proprietario. */
	SURVIVAL
}
