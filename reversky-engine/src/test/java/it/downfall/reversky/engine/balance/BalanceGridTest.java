package it.downfall.reversky.engine.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.rules.FactionBonus;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;

/** Le griglie del bilanciamento: la griglia base di {@code ENG-28} e quella delle forme del bonus di D-04. */
class BalanceGridTest {

	@Test
	@DisplayName("La griglia base è quella di BalanceConfig.grid() e scrive report-bilanciamento.md e report-carte.csv")
	void base_isTheEng28Grid() {
		assertThat(BalanceGrid.BASE.configs()).isEqualTo(BalanceConfig.grid());
		assertThat(BalanceGrid.BASE.reportFile()).isEqualTo("report-bilanciamento.md");
		assertThat(BalanceGrid.BASE.csvFile()).isEqualTo("report-carte.csv");
	}

	@Test
	@DisplayName("Griglia delle forme: +1 ATK contro +1 DEF, poi con +2 DEF a Wound e Invitation; 30 e 25 PV, 75 round")
	void forme_atkOneAgainstDefOneAndPerPair() {
		List<BalanceConfig> grid = BalanceGrid.FORME.configs();

		assertThat(grid).extracting(BalanceConfig::id)
				.containsExactly("pv30-atk1-def1", "pv25-atk1-def1", "pv30-atk1-def12", "pv25-atk1-def12");
		assertThat(grid).extracting(config -> config.rules().startingLifePoints()).containsExactly(30, 25, 30, 25);
		assertThat(grid).allSatisfy(config -> assertThat(config.rules().maxRounds()).isEqualTo(75));
		assertThat(grid).allSatisfy(config -> assertThat(config.factionBonuses()).hasSize(8));
		for (int faction : BalanceConfig.FACTIONS) {
			int beaten = BalanceConfig.beats(faction);
			assertThat(grid.getFirst().factionBonuses())
					.contains(new FactionBonus(faction, beaten, 1, 0), new FactionBonus(beaten, faction, 0, 1));
		}
		assertThat(grid.get(1).factionBonuses()).isEqualTo(grid.getFirst().factionBonuses());
		// Per coppia: chi è battuto da Archive (Wound) e da Dream (Invitation) ha +2 DEF, gli altri +1
		assertThat(grid.get(2).factionBonuses()).contains(
				new FactionBonus(TestFactions.WOUND, TestFactions.DREAM, 1, 0),
				new FactionBonus(TestFactions.DREAM, TestFactions.WOUND, 0, 1),
				new FactionBonus(TestFactions.INVITATION, TestFactions.ARCHIVE, 1, 0),
				new FactionBonus(TestFactions.ARCHIVE, TestFactions.INVITATION, 0, 1),
				new FactionBonus(TestFactions.ARCHIVE, TestFactions.WOUND, 1, 0),
				new FactionBonus(TestFactions.WOUND, TestFactions.ARCHIVE, 0, 2),
				new FactionBonus(TestFactions.DREAM, TestFactions.INVITATION, 1, 0),
				new FactionBonus(TestFactions.INVITATION, TestFactions.DREAM, 0, 2));
		assertThat(grid.getLast().factionBonuses()).isEqualTo(grid.get(2).factionBonuses());
		assertThat(BalanceGrid.FORME.reportFile()).isEqualTo("report-forme-bonus.md");
		assertThat(BalanceGrid.FORME.csvFile()).isEqualTo("report-forme-bonus-carte.csv");
	}

	@Test
	@DisplayName("Griglia a 30 PV (ENG-42): i tre bonus, D-04, di prova e +1 ATK contro +1 DEF, con il suo report")
	void pv30_threeBonusesAtThirtyLifePoints() {
		List<BalanceConfig> grid = BalanceGrid.PV30.configs();

		assertThat(grid).extracting(BalanceConfig::id).containsExactly("pv30-d04", "pv30-prova", "pv30-atk1-def1");
		assertThat(grid).allSatisfy(config -> assertThat(config.rules().startingLifePoints()).isEqualTo(30));
		assertThat(grid.get(0)).isEqualTo(BalanceGrid.BASE.configs().get(0));
		assertThat(grid.get(1)).isEqualTo(BalanceGrid.BASE.configs().get(1));
		assertThat(grid.get(2)).isEqualTo(BalanceGrid.FORME.configs().getFirst());
		assertThat(BalanceGrid.PV30.reportFile()).isEqualTo("report-30pv.md");
		assertThat(BalanceGrid.PV30.csvFile()).isEqualTo("report-30pv-carte.csv");
	}

	@Test
	@DisplayName("La griglia si sceglie per nome; un nome sconosciuto è un errore")
	void byName_knownAndUnknown() {
		assertThat(BalanceGrid.byName("base")).isEqualTo(BalanceGrid.BASE);
		assertThat(BalanceGrid.byName("forme")).isEqualTo(BalanceGrid.FORME);
		assertThat(BalanceGrid.byName("pv30")).isEqualTo(BalanceGrid.PV30);
		assertThatThrownBy(() -> BalanceGrid.byName("altro")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("altro");
	}

	@Test
	@DisplayName("Il report della griglia delle forme ha il suo titolo e rimanda al suo CSV")
	void report_titleAndCsvOfTheGrid() {
		CardDefinition x = TestCards.creature(1, "X", TestFactions.WOUND, null, "Common", 5, 5, null);
		BalanceReport report = new BalanceReport(List.of(x), 2, 20_261_120L, BalanceGrid.FORME);

		assertThat(report.markdown()).startsWith("# " + BalanceGrid.FORME.title() + "\n")
				.contains("`report-forme-bonus-carte.csv`").doesNotContain("`report-carte.csv`");
		assertThat(new BalanceReport(List.of(x), 2, 20_261_120L).markdown()).startsWith("# Report di bilanciamento\n")
				.contains("`report-carte.csv`");
	}
}
