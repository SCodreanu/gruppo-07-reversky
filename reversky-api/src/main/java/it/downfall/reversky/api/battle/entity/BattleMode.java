package it.downfall.reversky.api.battle.entity;

/**
 * Modalità di una partita (enum PostgreSQL {@code battle_mode}, docs/sfide).
 */
public enum BattleMode {

	/** Sfida tra amici su invito: niente rating né crediti. */
	FRIENDLY,

	/** Classificata dal matchmaking: aggiorna l'Elo e dà crediti. */
	RANKED
}
