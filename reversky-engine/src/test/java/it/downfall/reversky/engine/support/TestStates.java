package it.downfall.reversky.engine.support;

import java.util.List;
import java.util.Random;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
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
	 * Partita con le due creature già in campo, ognuna con il suo effetto dal registro (se la carta ha un codice), e i
	 * mazzi vuoti.
	 *
	 * @param effects            registro da cui creare gli effetti
	 * @param challengerCreature creatura dello sfidante
	 * @param opponentCreature   creatura dell'avversario
	 * @return lo stato
	 */
	public static MatchState inCombat(EffectRegistry effects, CardDefinition challengerCreature,
			CardDefinition opponentCreature) {
		MatchState state = withDecks(List.of(), List.of());
		state.challenger().placeCreature(withEffect(effects, challengerCreature, Side.CHALLENGER));
		state.opponent().placeCreature(withEffect(effects, opponentCreature, Side.OPPONENT));
		return state;
	}

	/**
	 * Creatura da mettere in campo con il suo effetto dal registro, come la pesca il motore.
	 *
	 * @param effects registro da cui creare l'effetto
	 * @param card    carta della creatura
	 * @param side    proprietario
	 * @return la creatura, senza effetto se la carta non ha codice
	 */
	public static CreatureInPlay withEffect(EffectRegistry effects, CardDefinition card, Side side) {
		String code = card.effectCode();
		return new CreatureInPlay(card, side, code == null ? null : effects.createCreature(code));
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
