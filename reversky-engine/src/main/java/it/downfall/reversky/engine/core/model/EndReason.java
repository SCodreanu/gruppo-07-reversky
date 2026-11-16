package it.downfall.reversky.engine.core.model;

/**
 * Motivo di fine partita. I valori sono nell'ordine in cui le condizioni si controllano dopo ogni round
 * (docs/regole-base, "Fine della partita").
 */
public enum EndReason {

	/** Condizione 1: entrambi i giocatori a 0 punti vita o meno, pareggio. */
	BOTH_LIFE_POINTS_ZERO,

	/** Condizione 2: un giocatore a 0 punti vita o meno, perde lui. */
	LIFE_POINTS_ZERO,

	/** Condizione 3: entrambi senza creature da schierare, vince chi ha più punti vita. */
	BOTH_DECKS_EMPTY,

	/** Condizione 4: un giocatore deve pescare ma non ha più creature, perde lui. */
	NO_CREATURES_LEFT,

	/** Condizione 5: raggiunto il limite di round, vince chi ha più punti vita. */
	ROUND_LIMIT
}
