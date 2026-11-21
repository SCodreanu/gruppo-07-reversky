package it.downfall.reversky.api.battle.entity;

/**
 * Risultato di una partita (enum PostgreSQL {@code battle_result}): gli stessi valori che restituisce l'engine.
 */
public enum BattleResult {
	CHALLENGER_WIN,
	OPPONENT_WIN,
	DRAW
}
