package it.downfall.reversky.engine.balance;

import java.util.List;

import it.downfall.reversky.engine.core.model.CardDefinition;

/**
 * Una partita del bilanciamento prima della simulazione: accoppiamento, seed e i due mazzi costruiti da quel seed.
 *
 * @param pairing        fazioni dello sfidante e dell'avversario
 * @param seed           seed della partita, per costruire i mazzi e per il mescolamento dell'engine
 * @param challengerDeck mazzo dello sfidante, prima del mescolamento
 * @param opponentDeck   mazzo dell'avversario, prima del mescolamento
 */
public record BalanceMatch(BalancePairing pairing, long seed, List<CardDefinition> challengerDeck,
		List<CardDefinition> opponentDeck) {

	/**
	 * Copia i mazzi.
	 */
	public BalanceMatch {
		challengerDeck = List.copyOf(challengerDeck);
		opponentDeck = List.copyOf(opponentDeck);
	}
}
