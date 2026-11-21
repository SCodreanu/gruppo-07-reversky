package it.downfall.reversky.engine.balance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.balance.BalanceReport.CardCheck;
import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.BattleResult;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.EndReason;
import it.downfall.reversky.engine.core.model.MatchResult;
import it.downfall.reversky.engine.core.model.PlayerResult;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;

class BalanceReportTest {

	private static final CardDefinition X = TestCards.creature(1, "X", TestFactions.WOUND, null, "Common", 5, 5,
			null);
	private static final CardDefinition Y = TestCards.creature(2, "Y", TestFactions.DREAM, null, "Common", 5, 5,
			null);
	private static final List<CardDefinition> CATALOG = List.of(X, Y);
	private static final BalanceConfig CONFIG = BalanceConfig.grid().getFirst();

	private BalanceStats stats;

	@BeforeEach
	void recordMatches() {
		// Wound batte Dream 4 a 0 (100%, fuori da 55-65); Wound e Invitation 1 a 1 (50%, nella banda neutra);
		// una partita su sei a ROUND_LIMIT
		stats = new BalanceStats(BalanceConfig.FACTIONS, 75);
		for (int match = 0; match < 4; match++) {
			stats.record(match(TestFactions.WOUND, TestFactions.DREAM), result(BattleResult.CHALLENGER_WIN,
					match == 0 ? EndReason.ROUND_LIMIT : EndReason.LIFE_POINTS_ZERO));
		}
		stats.record(match(TestFactions.WOUND, TestFactions.INVITATION),
				result(BattleResult.CHALLENGER_WIN, EndReason.LIFE_POINTS_ZERO));
		stats.record(match(TestFactions.INVITATION, TestFactions.WOUND),
				result(BattleResult.CHALLENGER_WIN, EndReason.LIFE_POINTS_ZERO));
	}

	@Test
	@DisplayName("Avvisi: scontro di vantaggio fuori da 55-65%, fazione fuori da 45-55%, ROUND_LIMIT sopra il 5%")
	void warnings_factionsAndRoundLimit() {
		List<String> warnings = BalanceReport.warnings(stats);

		assertThat(warnings).anyMatch(warning -> warning.contains("Wound contro Dream")
				&& warning.contains("100,0%") && warning.contains("55-65%"));
		assertThat(warnings).noneMatch(warning -> warning.contains("Wound contro Invitation"));
		assertThat(warnings).anyMatch(warning -> warning.contains("Dream") && warning.contains("45-55%"));
		assertThat(warnings).anyMatch(warning -> warning.contains("ROUND_LIMIT") && warning.contains("16,7%"));
	}

	@Test
	@DisplayName("Primo giocatore: tutte e sei vinte dallo sfidante, fuori da 45-55%")
	void warnings_firstPlayer() {
		assertThat(BalanceReport.warnings(stats)).anyMatch(warning -> warning.contains("primo giocatore")
				&& warning.contains("100,0%"));
	}

	@Test
	@DisplayName("Carta: fuori banda oltre 10 punti dalla media del suo gruppo; sotto 500 partite campione basso")
	void cardCheck_bandAndSample() {
		assertThat(BalanceReport.cardCheck(new BalanceStats.Score(600, 400, 0), 0.5)).isEqualTo(CardCheck.ABOVE);
		assertThat(BalanceReport.cardCheck(new BalanceStats.Score(600, 200, 0), 0.5)).isEqualTo(CardCheck.BELOW);
		assertThat(BalanceReport.cardCheck(new BalanceStats.Score(600, 300, 0), 0.55)).isEqualTo(CardCheck.OK);
		assertThat(BalanceReport.cardCheck(new BalanceStats.Score(499, 499, 0), 0.5))
				.isEqualTo(CardCheck.LOW_SAMPLE);
	}

	@Test
	@DisplayName("La scala delle rarità delle creature deve crescere: Common < Uncommon < Rare < Mythic < Legendary")
	void rarityScale_flagsADrop() {
		Map<String, Double> averages = new LinkedHashMap<>();
		averages.put("Common", 0.45);
		averages.put("Uncommon", 0.48);
		averages.put("Rare", 0.47);
		averages.put("Mythic", 0.52);
		averages.put("Legendary", 0.55);

		assertThat(BalanceReport.rarityScaleWarnings(averages))
				.containsExactly("Rare (47,0%) non supera Uncommon (48,0%)");
	}

	@Test
	@DisplayName("Media del gruppo nel mazzo e da giocata: le due Common X e Y nel mazzo, solo X giocata")
	void groupRates_inDeckAndPlayed() {
		// Arrange: X è sempre dello sfidante, che vince tutte e sei le partite; Y non è mai giocata
		Map<String, Double> inDeck = BalanceReport.groupRates(stats, CATALOG, BalanceReport.Basis.IN_DECK);
		Map<String, Double> played = BalanceReport.groupRates(stats, CATALOG, BalanceReport.Basis.PLAYED);

		// Assert: nel mazzo 12 partite Common, 6 vinte; giocata solo X, 6 su 6
		assertThat(inDeck.get("Common")).isEqualTo(0.5);
		assertThat(played.get("Common")).isEqualTo(1.0);
	}

	@Test
	@DisplayName("Il report spiega che il punteggio da giocata è spostato in basso; la scala usa quello nel mazzo")
	void markdown_explainsPlayedBias() {
		BalanceReport report = new BalanceReport(CATALOG, 2, 20_261_120L);
		report.add(CONFIG, stats);

		assertThat(report.markdown()).contains("chi perde gioca più carte").contains("| Nel mazzo | Da giocata |");
	}

	@Test
	@DisplayName("Report deterministico: due generazioni uguali; CSV con una riga per carta e configurazione")
	void markdownAndCsv_deterministic() {
		BalanceReport report = new BalanceReport(CATALOG, 2, 20_261_120L);
		report.add(CONFIG, stats);
		BalanceReport again = new BalanceReport(CATALOG, 2, 20_261_120L);
		again.add(CONFIG, stats);

		assertThat(report.markdown()).isEqualTo(again.markdown()).contains("# Report di bilanciamento")
				.contains(CONFIG.label());
		List<String> csvLines = report.csv().lines().toList();
		assertThat(csvLines).hasSize(1 + CATALOG.size());
		assertThat(csvLines.getFirst()).startsWith("configurazione,id,nome,");
		assertThat(csvLines.get(1)).startsWith(CONFIG.id() + ",1,X,");
	}

	private static BalanceMatch match(int challenger, int opponent) {
		return new BalanceMatch(new BalancePairing(0, challenger, opponent), 1L, List.of(X), List.of(Y));
	}

	private static MatchResult result(BattleResult outcome, EndReason reason) {
		List<MatchEvent> log = List.of(new MatchEvent(0, 1, EventType.CREATURE_ENTERED, Side.CHALLENGER,
				Map.of("card", "X"), "X"), new MatchEvent(1, 10, EventType.MATCH_ENDED, null, Map.of(), "end"));
		return new MatchResult(outcome, reason, 10, new PlayerResult(7, 0, 0), new PlayerResult(12, 0, 0), 1L,
				"1.0", log);
	}
}
