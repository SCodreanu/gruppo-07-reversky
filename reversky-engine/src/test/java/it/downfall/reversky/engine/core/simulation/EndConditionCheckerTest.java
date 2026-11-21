package it.downfall.reversky.engine.core.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.model.BattleResult;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.EndReason;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestRules;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Test di {@link EndConditionChecker}: le 5 condizioni di fine partita e il loro ordine (docs/regole-base, "Fine
 * della partita"; D-02, D-05). Ogni test usa un limite di 3 round, così il limite si raggiunge in pochi passi.
 */
class EndConditionCheckerTest {

	private static final int MAX_ROUNDS = 3;
	private static final CardDefinition CRAWLERS = TestCards.creature("Crawlers", TestFactions.WOUND, 4, 5);
	private static final CardDefinition KAPPA = TestCards.creature("Kappa", TestFactions.DREAM, 6, 6);
	private static final CardDefinition VEIL = TestCards.spell(96, "Veil of the Anchored", CardSubtype.SHIELD,
			"VEIL_OF_ANCHORED");

	private final EndConditionChecker checker = new EndConditionChecker(TestRules.of(30, MAX_ROUNDS));

	@Test
	@DisplayName("Entrambi con creature in campo, PV sopra 0 e round sotto il limite: la partita continua")
	void check_nothingReached_continues() {
		// Arrange
		MatchState state = TestStates.inCombat(CRAWLERS, KAPPA);
		state.startNextRound();

		// Act + Assert
		assertThat(checker.check(state)).isEmpty();
	}

	@Test
	@DisplayName("1. Entrambi a 0 PV o meno (0 e −4) → DRAW, BOTH_LIFE_POINTS_ZERO")
	void check_bothLifePointsZero_draw() {
		// Arrange
		MatchState state = TestStates.inCombat(CRAWLERS, KAPPA);
		state.challenger().loseLifePoints(30);
		state.opponent().loseLifePoints(34);

		// Act + Assert
		assertThat(checker.check(state)).contains(new MatchEnd(BattleResult.DRAW, EndReason.BOTH_LIFE_POINTS_ZERO));
	}

	@Test
	@DisplayName("2. Un giocatore a 0 PV o meno perde e vince l'altro, sia lo sfidante sia l'avversario")
	void check_oneLifePointsZero_otherWins() {
		// Arrange
		MatchState challengerDown = TestStates.inCombat(CRAWLERS, KAPPA);
		challengerDown.challenger().loseLifePoints(30);
		MatchState opponentDown = TestStates.inCombat(CRAWLERS, KAPPA);
		opponentDown.opponent().loseLifePoints(35);

		// Act + Assert
		assertThat(checker.check(challengerDown))
				.contains(new MatchEnd(BattleResult.OPPONENT_WIN, EndReason.LIFE_POINTS_ZERO));
		assertThat(checker.check(opponentDown))
				.contains(new MatchEnd(BattleResult.CHALLENGER_WIN, EndReason.LIFE_POINTS_ZERO));
	}

	@Test
	@DisplayName("2 prima di 4 (esempio 10): l'avversario a −5 PV e senza creature perde per LIFE_POINTS_ZERO")
	void check_lifePointsZeroAndNoCreatures_lifePointsWins() {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(), List.of());
		state.challenger().placeCreature(new CreatureInPlay(CRAWLERS, Side.CHALLENGER));
		state.opponent().loseLifePoints(35);

		// Act + Assert
		assertThat(checker.check(state)).contains(new MatchEnd(BattleResult.CHALLENGER_WIN,
				EndReason.LIFE_POINTS_ZERO));
	}

	@Test
	@DisplayName("3. Entrambi senza creature da schierare, PV pari → DRAW, BOTH_DECKS_EMPTY (esempio 03)")
	void check_bothWithoutCreaturesSameLifePoints_draw() {
		// Arrange: solo una Spell rimasta, che non è una creatura
		MatchState state = TestStates.withDecks(List.of(VEIL), List.of());

		// Act + Assert
		assertThat(checker.check(state)).contains(new MatchEnd(BattleResult.DRAW, EndReason.BOTH_DECKS_EMPTY));
	}

	@Test
	@DisplayName("3. Entrambi senza creature: vince chi ha più PV (28 contro 27)")
	void check_bothWithoutCreatures_moreLifePointsWins() {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(), List.of());
		state.challenger().loseLifePoints(2);
		state.opponent().loseLifePoints(3);

		// Act + Assert
		assertThat(checker.check(state)).contains(new MatchEnd(BattleResult.CHALLENGER_WIN,
				EndReason.BOTH_DECKS_EMPTY));
	}

	@Test
	@DisplayName("4. Un giocatore senza creature (né in campo né nel mazzo) perde se l'altro ne ha (esempio 01)")
	void check_oneWithoutCreatures_otherWins() {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(), List.of(VEIL));
		state.challenger().placeCreature(new CreatureInPlay(CRAWLERS, Side.CHALLENGER));

		// Act + Assert
		assertThat(checker.check(state)).contains(new MatchEnd(BattleResult.CHALLENGER_WIN,
				EndReason.NO_CREATURES_LEFT));
	}

	@Test
	@DisplayName("4. Una creatura ancora nel mazzo conta: l'avversario con Kappa nel mazzo vince")
	void check_creatureOnlyInDeck_stillHasCreatures() {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(), List.of(VEIL, KAPPA));

		// Act + Assert
		assertThat(checker.check(state)).contains(new MatchEnd(BattleResult.OPPONENT_WIN,
				EndReason.NO_CREATURES_LEFT));
	}

	@Test
	@DisplayName("5. Raggiunto il limite di round (3), PV pari → DRAW, ROUND_LIMIT (esempio 06)")
	void check_roundLimitSameLifePoints_draw() {
		// Arrange
		MatchState state = TestStates.inCombat(CRAWLERS, KAPPA);
		playRounds(state, MAX_ROUNDS);

		// Act + Assert
		assertThat(checker.check(state)).contains(new MatchEnd(BattleResult.DRAW, EndReason.ROUND_LIMIT));
	}

	@Test
	@DisplayName("5. Al limite vince chi ha più PV: 30 contro 29 → CHALLENGER_WIN (esempio 07); al round 2 si continua")
	void check_roundLimit_moreLifePointsWins() {
		// Arrange
		MatchState state = TestStates.inCombat(CRAWLERS, KAPPA);
		state.opponent().loseLifePoints(1);
		playRounds(state, MAX_ROUNDS - 1);

		// Act + Assert
		assertThat(checker.check(state)).isEmpty();
		state.startNextRound();
		assertThat(checker.check(state)).contains(new MatchEnd(BattleResult.CHALLENGER_WIN, EndReason.ROUND_LIMIT));
	}

	@Test
	@DisplayName("3 prima di 5: all'ultimo round utile senza creature per nessuno finisce per BOTH_DECKS_EMPTY")
	void check_bothDecksEmptyAtRoundLimit_bothDecksEmptyWins() {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(), List.of());
		playRounds(state, MAX_ROUNDS);

		// Act + Assert
		assertThat(checker.check(state)).contains(new MatchEnd(BattleResult.DRAW, EndReason.BOTH_DECKS_EMPTY));
	}

	private static void playRounds(MatchState state, int rounds) {
		for (int i = 0; i < rounds; i++) {
			state.startNextRound();
		}
	}
}
