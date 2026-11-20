package it.downfall.reversky.engine.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.BattleResult;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.EndReason;
import it.downfall.reversky.engine.core.model.MatchResult;
import it.downfall.reversky.engine.core.model.PlayerResult;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;

/**
 * Tre partite costruite a mano: Wound contro Dream vinta dallo sfidante in 10 round, Dream contro Wound pari a
 * {@code ROUND_LIMIT} (75 round), Wound contro Wound vinta dall'avversario in 20 round.
 */
class BalanceStatsTest {

	private static final CardDefinition X = TestCards.creature(1, "X", TestFactions.WOUND, null, "Common", 5, 5,
			null);
	private static final CardDefinition Y = TestCards.creature(2, "Y", TestFactions.DREAM, null, "Common", 5, 5,
			null);
	private static final CardDefinition S = TestCards.spell(3, "S", CardSubtype.DAMAGE, "SKIN_FRACTURE");

	private BalanceStats stats;

	@BeforeEach
	void recordThreeMatches() {
		stats = new BalanceStats(BalanceConfig.FACTIONS, 75);
		stats.record(match(TestFactions.WOUND, TestFactions.DREAM, List.of(X, X, S), List.of(Y)),
				result(BattleResult.CHALLENGER_WIN, EndReason.LIFE_POINTS_ZERO, 10,
						event(EventType.CREATURE_ENTERED, Side.CHALLENGER, "X"),
						event(EventType.SPELL_RESOLVED, Side.CHALLENGER, "S"),
						event(EventType.EFFECT_TRIGGERED, Side.CHALLENGER, "X"),
						event(EventType.EFFECT_TRIGGERED, Side.CHALLENGER, "X")));
		stats.record(match(TestFactions.DREAM, TestFactions.WOUND, List.of(Y), List.of(X)),
				result(BattleResult.DRAW, EndReason.ROUND_LIMIT, 75,
						event(EventType.CREATURE_ENTERED, Side.OPPONENT, "X"),
						event(EventType.CREATURE_ENTERED, Side.CHALLENGER, "Y")));
		stats.record(match(TestFactions.WOUND, TestFactions.WOUND, List.of(X), List.of(X)),
				result(BattleResult.OPPONENT_WIN, EndReason.NO_CREATURES_LEFT, 20,
						event(EventType.CREATURE_ENTERED, Side.CHALLENGER, "X"),
						event(EventType.CREATURE_ENTERED, Side.OPPONENT, "X")));
	}

	@Test
	@DisplayName("Conta le partite, i motivi di fine e la durata (media, minimo, mediana, p90, massimo)")
	void record_countsEndReasonsAndLength() {
		assertThat(stats.matches()).isEqualTo(3);
		assertThat(stats.endReasonCount(EndReason.LIFE_POINTS_ZERO)).isEqualTo(1);
		assertThat(stats.endReasonCount(EndReason.ROUND_LIMIT)).isEqualTo(1);
		assertThat(stats.endReasonCount(EndReason.NO_CREATURES_LEFT)).isEqualTo(1);
		assertThat(stats.endReasonCount(EndReason.BOTH_DECKS_EMPTY)).isZero();
		assertThat(stats.averageRounds()).isCloseTo(35.0, within(1e-9));
		assertThat(stats.shortestMatch()).isEqualTo(10);
		assertThat(stats.roundsPercentile(50)).isEqualTo(20);
		assertThat(stats.roundsPercentile(90)).isEqualTo(75);
		assertThat(stats.longestMatch()).isEqualTo(75);
	}

	@Test
	@DisplayName("Fazioni e scontri diretti senza le partite speculari; il pareggio vale mezza vittoria")
	void record_factionScoresExcludeMirrors() {
		assertThat(stats.faction(TestFactions.WOUND)).isEqualTo(new BalanceStats.Score(2, 1, 1));
		assertThat(stats.faction(TestFactions.WOUND).rate()).isCloseTo(0.75, within(1e-9));
		assertThat(stats.faction(TestFactions.DREAM)).isEqualTo(new BalanceStats.Score(2, 0, 1));
		assertThat(stats.matchup(TestFactions.WOUND, TestFactions.DREAM)).isEqualTo(new BalanceStats.Score(2, 1, 1));
		assertThat(stats.matchup(TestFactions.DREAM, TestFactions.WOUND)).isEqualTo(new BalanceStats.Score(2, 0, 1));
		assertThat(stats.matchup(TestFactions.WOUND, TestFactions.WOUND).matches()).isZero();
		assertThat(stats.faction(TestFactions.ARCHIVE).matches()).isZero();
	}

	@Test
	@DisplayName("Il primo giocatore (lo sfidante, pesca per primo) conta tutte le partite, anche le speculari")
	void record_firstPlayerScore() {
		assertThat(stats.firstPlayer()).isEqualTo(new BalanceStats.Score(3, 1, 1));
		assertThat(stats.firstPlayer().rate()).isCloseTo(0.5, within(1e-9));
	}

	@Test
	@DisplayName("Una carta conta una volta per lato, nel mazzo e se giocata; le attivazioni si sommano")
	void record_cardScores() {
		assertThat(stats.inDeck(X.cardId())).isEqualTo(new BalanceStats.Score(4, 2, 1));
		assertThat(stats.played(X.cardId())).isEqualTo(new BalanceStats.Score(4, 2, 1));
		assertThat(stats.inDeck(Y.cardId())).isEqualTo(new BalanceStats.Score(2, 0, 1));
		assertThat(stats.played(Y.cardId())).isEqualTo(new BalanceStats.Score(1, 0, 1));
		assertThat(stats.played(S.cardId())).isEqualTo(new BalanceStats.Score(1, 1, 0));
		assertThat(stats.triggers(X.cardId())).isEqualTo(2);
		assertThat(stats.triggers(S.cardId())).isEqualTo(1);
		assertThat(stats.played(99).matches()).isZero();
	}

	private static BalanceMatch match(int challenger, int opponent, List<CardDefinition> challengerDeck,
			List<CardDefinition> opponentDeck) {
		return new BalanceMatch(new BalancePairing(0, challenger, opponent), 1L, challengerDeck, opponentDeck);
	}

	private static MatchResult result(BattleResult outcome, EndReason reason, int rounds, MatchEvent... events) {
		List<MatchEvent> log = new ArrayList<>(List.of(events));
		log.add(new MatchEvent(log.size(), rounds, EventType.MATCH_ENDED, null, Map.of(), "end"));
		return new MatchResult(outcome, reason, rounds, new PlayerResult(7, 0, 0), new PlayerResult(12, 0, 0), 1L,
				"1.0", log);
	}

	private static MatchEvent event(EventType type, Side side, String card) {
		return new MatchEvent(0, 1, type, side, Map.of("card", card), card);
	}
}
