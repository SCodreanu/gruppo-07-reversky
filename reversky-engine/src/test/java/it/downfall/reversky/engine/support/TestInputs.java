package it.downfall.reversky.engine.support;

import java.util.List;

import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.PlayerInput;
import it.downfall.reversky.engine.core.model.SimulationInput;

/**
 * Input di simulazione di prova: regole {@link TestRules#standard()}, le quattro fazioni e i bonus di D-04.
 */
public final class TestInputs {

	/** Seed dei test quando il test non ne sceglie uno. */
	public static final long DEFAULT_SEED = 42L;

	/** Id utente dello sfidante, come negli esempi di docs/api-engine. */
	public static final int CHALLENGER_USER_ID = 7;

	/** Id utente dell'avversario, come negli esempi di docs/api-engine. */
	public static final int OPPONENT_USER_ID = 12;

	private TestInputs() {
	}

	/**
	 * Input standard con i due mazzi dati e il seed {@link #DEFAULT_SEED}.
	 *
	 * @param challengerDeck mazzo dello sfidante
	 * @param opponentDeck   mazzo dell'avversario
	 * @return l'input
	 */
	public static SimulationInput of(List<CardDefinition> challengerDeck, List<CardDefinition> opponentDeck) {
		return of(DEFAULT_SEED, challengerDeck, opponentDeck);
	}

	/**
	 * Input standard con i due mazzi e il seed dati.
	 *
	 * @param seed           seme del mescolamento
	 * @param challengerDeck mazzo dello sfidante
	 * @param opponentDeck   mazzo dell'avversario
	 * @return l'input
	 */
	public static SimulationInput of(long seed, List<CardDefinition> challengerDeck,
			List<CardDefinition> opponentDeck) {
		return new SimulationInput(seed, TestRules.standard(), TestFactions.all(), TestFactions.standardBonuses(),
				new PlayerInput(CHALLENGER_USER_ID, challengerDeck), new PlayerInput(OPPONENT_USER_ID, opponentDeck));
	}
}
