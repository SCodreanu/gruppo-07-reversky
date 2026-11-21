package it.downfall.reversky.engine.balance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.MatchResult;
import it.downfall.reversky.engine.support.TestFactions;

/** Il runner con poche partite vere: gira nella build normale e prova seed, mazzi, invarianti e determinismo. */
class BalanceRunnerTest {

	private static final int MATCHES_PER_PAIRING = 2;

	private static List<CardDefinition> catalog;

	@BeforeAll
	static void loadCatalog() throws IOException {
		catalog = CatalogCsv.load(CatalogCsv.DEFAULT_PATH);
	}

	@Test
	@DisplayName("16 accoppiamenti ordinati di fazioni, speculari compresi: Wound, Invitation, Archive, Dream")
	void pairings_allSixteen() {
		List<BalancePairing> pairings = BalancePairing.all();

		assertThat(pairings).hasSize(16);
		assertThat(pairings.getFirst()).isEqualTo(new BalancePairing(0, TestFactions.WOUND, TestFactions.WOUND));
		assertThat(pairings.get(1)).isEqualTo(new BalancePairing(1, TestFactions.WOUND, TestFactions.INVITATION));
		assertThat(pairings.getLast()).isEqualTo(new BalancePairing(15, TestFactions.DREAM, TestFactions.DREAM));
	}

	@Test
	@DisplayName("Seed della partita i dell'accoppiamento p: base + p x partite per accoppiamento + i")
	void match_seedAndDecks() {
		BalanceRunner runner = new BalanceRunner(catalog, MATCHES_PER_PAIRING, 100L);
		BalancePairing pairing = BalancePairing.all().get(3);

		BalanceMatch match = runner.match(pairing, 1);

		assertThat(match.seed()).isEqualTo(100L + 3 * MATCHES_PER_PAIRING + 1);
		assertThat(match.challengerDeck()).hasSize(30)
				.filteredOn(card -> card.factionId() != null).allSatisfy(card -> assertThat(card.factions())
						.contains(pairing.challengerFactionId()));
		assertThat(match.opponentDeck()).filteredOn(card -> card.factionId() != null)
				.allSatisfy(card -> assertThat(card.factions()).contains(pairing.opponentFactionId()));
		assertThat(runner.match(pairing, 1)).isEqualTo(match);
	}

	@Test
	@DisplayName("Una configurazione gioca 16 x n partite, tutte finite entro il limite di round, ripetibili")
	void run_everyMatchEndsAndIsDeterministic() {
		BalanceRunner runner = new BalanceRunner(catalog, MATCHES_PER_PAIRING, BalanceRunner.BASE_SEED);
		BalanceConfig config = BalanceConfig.grid().getFirst();

		BalanceStats stats = runner.run(config, 4);
		MatchResult result = runner.simulate(config, runner.match(BalancePairing.all().get(5), 0));

		assertThat(stats.matches()).isEqualTo(16 * MATCHES_PER_PAIRING);
		assertThat(stats.longestMatch()).isLessThanOrEqualTo(config.rules().maxRounds());
		assertThat(result.log().getLast().type()).isEqualTo(EventType.MATCH_ENDED);
		assertThat(runner.simulate(config, runner.match(BalancePairing.all().get(5), 0))).isEqualTo(result);
	}

	@Test
	@DisplayName("La griglia ha 4 configurazioni: 30 e 25 punti vita, bonus D-04 e bonus di prova, 75 round")
	void grid_fourConfigs() {
		List<BalanceConfig> grid = BalanceConfig.grid();

		assertThat(grid).extracting(BalanceConfig::id)
				.containsExactly("pv30-d04", "pv30-prova", "pv25-d04", "pv25-prova");
		assertThat(grid).allSatisfy(config -> assertThat(config.rules().maxRounds()).isEqualTo(75));
		assertThat(grid.get(0).factionBonuses()).isEqualTo(TestFactions.standardBonuses());
		assertThat(grid.get(1).factionBonuses()).hasSize(4)
				.allSatisfy(bonus -> assertThat(bonus.atkBonus()).isZero())
				.allSatisfy(bonus -> assertThat(bonus.defBonus()).isEqualTo(1))
				.allSatisfy(bonus -> assertThat(BalanceConfig.beats(bonus.originFactionId()))
						.isEqualTo(bonus.targetFactionId()));
		assertThat(grid.get(2).rules().startingLifePoints()).isEqualTo(25);
	}
}
