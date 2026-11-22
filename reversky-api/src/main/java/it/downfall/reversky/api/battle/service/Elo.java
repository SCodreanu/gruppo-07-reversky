package it.downfall.reversky.api.battle.service;

import it.downfall.reversky.api.battle.dto.BattleOutcome;

/**
 * Calcolo dell'Elo delle partite classificate (docs/sfide, "Elo — player_ratings"), approvato dal cliente.
 * <p>
 * Funzioni pure, senza database: chi aggiorna i rating le chiama per entrambi i giocatori con il rating e le partite
 * giocate <b>prima</b> della partita, poi applica le variazioni con {@code PlayerRating.recordGame}. Se i due
 * giocatori hanno K diversi, le due variazioni non sono uguali e opposte.
 */
public final class Elo {

	/** Fino a questo numero di partite classificate giocate vale {@link #K_NEW_PLAYER}. */
	static final int NEW_PLAYER_GAMES = 10;

	/** K nelle prime 10 partite classificate: il rating si assesta più in fretta. */
	static final int K_NEW_PLAYER = 40;

	/** K dalla 11ª partita classificata in poi. */
	static final int K_ESTABLISHED = 20;

	private Elo() {
	}

	/**
	 * Variazione Elo di un giocatore dopo una partita classificata: {@code round(K × (punteggio − E))}, con
	 * {@code E = 1 / (1 + 10^((R_avversario − R) / 400))}. Il rating non scende mai sotto 0.
	 *
	 * @param rating         rating del giocatore prima della partita
	 * @param opponentRating rating dell'avversario prima della partita
	 * @param score          1 vittoria, 0,5 pareggio, 0 sconfitta ({@link #score(BattleOutcome)})
	 * @param gamesPlayed    partite classificate giocate prima di questa
	 * @return la variazione da sommare al rating, mai minore di {@code -rating}
	 */
	public static int eloDelta(int rating, int opponentRating, double score, int gamesPlayed) {
		double expected = 1.0 / (1.0 + Math.pow(10, (opponentRating - rating) / 400.0));
		int k = gamesPlayed < NEW_PLAYER_GAMES ? K_NEW_PLAYER : K_ESTABLISHED;
		int delta = (int) Math.round(k * (score - expected));
		// il rating non scende sotto 0 (chk_player_ratings_rating_nonnegative)
		return Math.max(delta, -rating);
	}

	/**
	 * Punteggio Elo di un esito: tutti i pareggi del regolamento (limite di round, entrambi a zero punti vita) valgono
	 * 0,5.
	 *
	 * @param outcome esito dal punto di vista del giocatore
	 * @return 1, 0,5 o 0
	 */
	public static double score(BattleOutcome outcome) {
		return switch (outcome) {
			case WIN -> 1.0;
			case DRAW -> 0.5;
			case LOSS -> 0.0;
		};
	}
}
