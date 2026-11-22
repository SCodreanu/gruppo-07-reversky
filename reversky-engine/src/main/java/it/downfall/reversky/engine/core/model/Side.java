package it.downfall.reversky.engine.core.model;

/**
 * Lato di un giocatore nella partita. Un evento che riguarda entrambi i giocatori ha {@code side} nullo,
 * non un terzo valore (docs/log-partita).
 */
public enum Side {

	/** Chi ha lanciato la sfida. */
	CHALLENGER,

	/** Chi ha accettato la sfida. */
	OPPONENT
}
