package it.downfall.reversky.engine.core.model;

/**
 * Esito di una partita, uguale all'enum {@code BattleResult} del database dell'API.
 */
public enum BattleResult {

	/** Vince lo sfidante. */
	CHALLENGER_WIN,

	/** Vince l'avversario. */
	OPPONENT_WIN,

	/** Pareggio. */
	DRAW
}
