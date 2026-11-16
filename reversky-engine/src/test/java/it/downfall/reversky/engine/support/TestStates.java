package it.downfall.reversky.engine.support;

import java.util.List;
import java.util.Random;

import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PlayerState;
import it.downfall.reversky.engine.core.model.Side;

/**
 * Stati di partita di prova, con i punti vita di {@link TestRules#standard()}, gli id utente di {@link TestInputs} e
 * un generatore creato da {@link TestInputs#DEFAULT_SEED}.
 */
public final class TestStates {

	private TestStates() {
	}

	/**
	 * Partita con le due creature già in campo e i mazzi vuoti, per i test di un round.
	 *
	 * @param challengerCreature creatura dello sfidante
	 * @param opponentCreature   creatura dell'avversario
	 * @return lo stato
	 */
	public static MatchState inCombat(CardDefinition challengerCreature, CardDefinition opponentCreature) {
		MatchState state = withDecks(List.of(), List.of());
		state.challenger().placeCreature(new CreatureInPlay(challengerCreature, Side.CHALLENGER));
		state.opponent().placeCreature(new CreatureInPlay(opponentCreature, Side.OPPONENT));
		return state;
	}

	/**
	 * Partita prima della pescata iniziale, con i mazzi nell'ordine dato (non mescolati).
	 *
	 * @param challengerDeck mazzo dello sfidante
	 * @param opponentDeck   mazzo dell'avversario
	 * @return lo stato al round 0, senza creature in campo
	 */
	public static MatchState withDecks(List<CardDefinition> challengerDeck, List<CardDefinition> opponentDeck) {
		return new MatchState(
				new PlayerState(Side.CHALLENGER, TestInputs.CHALLENGER_USER_ID, TestRules.STARTING_LIFE_POINTS,
						challengerDeck),
				new PlayerState(Side.OPPONENT, TestInputs.OPPONENT_USER_ID, TestRules.STARTING_LIFE_POINTS,
						opponentDeck),
				new Random(TestInputs.DEFAULT_SEED));
	}
}
