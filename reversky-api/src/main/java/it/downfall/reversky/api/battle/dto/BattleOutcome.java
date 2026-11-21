package it.downfall.reversky.api.battle.dto;

import it.downfall.reversky.api.battle.entity.BattleResult;

/**
 * Esito di una partita dal punto di vista di chi la guarda (docs/sfide, "Storico").
 */
public enum BattleOutcome {
	WIN,
	LOSS,
	DRAW;

	/**
	 * @param asChallenger true se chi guarda è lo sfidante
	 * @return l'esito per chi guarda, oppure null se la partita non è completata
	 */
	public static BattleOutcome of(BattleResult result, boolean asChallenger) {
		if (result == null) {
			return null;
		}
		return switch (result) {
			case DRAW -> DRAW;
			case CHALLENGER_WIN -> asChallenger ? WIN : LOSS;
			case OPPONENT_WIN -> asChallenger ? LOSS : WIN;
		};
	}
}
