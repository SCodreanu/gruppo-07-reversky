package it.downfall.reversky.engine.core.model;

/**
 * Statistica su cui agisce un {@link StatModifier}.
 */
public enum Stat {

	/** Si somma all'attacco in ogni round in cui il modificatore è attivo. */
	ATTACK,

	/**
	 * DEF temporanea che funziona da scudo (D-35): assorbe i danni prima della DEF della creatura e alla scadenza
	 * sparisce solo la parte non usata. La DEF permanente non è un modificatore: si somma alla DEF.
	 */
	DEFENSE_SHIELD
}
