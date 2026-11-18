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
	DEFENSE_SHIELD,

	/**
	 * Attacco congelato (Yuki-onna, deciso il 18 novembre): finché il modificatore è attivo l'attacco della creatura
	 * è 0, dopo tutti gli altri modificatori e l'effetto dell'attaccante. Non ha un valore.
	 */
	ATTACK_FREEZE
}
