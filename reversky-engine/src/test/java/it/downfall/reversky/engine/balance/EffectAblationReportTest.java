package it.downfall.reversky.engine.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.balance.EffectAblation.Measure;
import it.downfall.reversky.engine.balance.EffectAblationReport.Row;
import it.downfall.reversky.engine.balance.EffectAblationReport.Verdict;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;

/**
 * Test del report del valore degli effetti ({@code ENG-42}, fase 2): taratura in punti per V, giudizio contro il PS
 * per le creature e contro la media del tipo per le altre carte (scelto il 21 novembre).
 */
class EffectAblationReportTest {

	private static final int WOUND = TestFactions.WOUND;

	@Test
	@DisplayName("Punti per V: media dei guadagni con +1 DEF delle creature senza effetto")
	void pointsPerV_averageOfCalibration() {
		EffectAblationReport report = report();
		report.addCalibration(vanilla(1), new Measure(100, 0.008, 0.002));
		report.addCalibration(vanilla(2), new Measure(100, 0.012, 0.002));

		assertThat(report.pointsPerV()).isCloseTo(0.010, within(1e-12));
	}

	@Test
	@DisplayName("Creature: troppo forte o debole se lontana dal PS più di 2 V e più del 50%; altrimenti ok")
	void creatures_judgedAgainstPrice() {
		EffectAblationReport report = calibrated();
		report.addEffect(creature(10, "Strong"), 3.0, new Measure(50, 0.08, 0.01));
		report.addEffect(creature(11, "Fair"), 3.0, new Measure(50, 0.04, 0.01));
		report.addEffect(creature(12, "Weak"), 6.0, new Measure(50, 0.01, 0.01));
		report.addEffect(creature(13, "Close"), 10.0, new Measure(50, 0.13, 0.01));

		Map<String, Row> rows = rowsByName(report);

		assertThat(rows.get("Strong").valueV()).isCloseTo(8.0, within(1e-9));
		assertThat(rows.get("Strong").errorV()).isCloseTo(1.0, within(1e-9));
		assertThat(rows.get("Strong").reference()).isEqualTo(3.0);
		assertThat(rows.get("Strong").gapV()).isCloseTo(5.0, within(1e-9));
		assertThat(rows.get("Strong").verdict()).isEqualTo(Verdict.STRONG);
		assertThat(rows.get("Fair").verdict()).isEqualTo(Verdict.OK);
		assertThat(rows.get("Weak").verdict()).isEqualTo(Verdict.WEAK);
		// 3 V sopra il PS, ma meno del 50% di 10
		assertThat(rows.get("Close").verdict()).isEqualTo(Verdict.OK);
	}

	@Test
	@DisplayName("Spell, Hidden, Terreni e Oggetti: riferimento la media in V del loro tipo")
	void nonCreatures_judgedAgainstTypeAverage() {
		EffectAblationReport report = calibrated();
		report.addEffect(spell(20, "Spell A"), 4.0, new Measure(50, 0.05, 0.01));
		report.addEffect(spell(21, "Spell B"), 4.0, new Measure(50, 0.05, 0.01));
		report.addEffect(spell(22, "Spell C"), 4.0, new Measure(50, 0.11, 0.01));

		Map<String, Row> rows = rowsByName(report);

		assertThat(rows.get("Spell C").reference()).isCloseTo(7.0, within(1e-9));
		assertThat(rows.get("Spell C").verdict()).isEqualTo(Verdict.STRONG);
		// 2 V sotto la media: non più di 2
		assertThat(rows.get("Spell A").verdict()).isEqualTo(Verdict.OK);
	}

	@Test
	@DisplayName("Senza taratura il report non si può calcolare")
	void rows_needCalibration() {
		EffectAblationReport report = report();
		report.addEffect(creature(10, "Strong"), 3.0, new Measure(50, 0.08, 0.01));

		assertThatThrownBy(report::rows).isInstanceOf(IllegalStateException.class).hasMessageContaining("taratura");
	}

	@Test
	@DisplayName("Markdown con taratura, tabelle per tipo e avvisi; CSV con una riga per carta")
	void markdownAndCsv() {
		EffectAblationReport report = calibrated();
		report.addEffect(creature(10, "Strong"), 3.0, new Measure(50, 0.08, 0.01));
		report.addEffect(spell(20, "Spell A"), 4.0, new Measure(50, 0.05, 0.01));

		String markdown = report.markdown();
		String csv = report.csv();

		assertThat(markdown).startsWith("# Report del valore degli effetti")
				.contains("## Taratura", "1,00 punti", "## Creature", "## Spell", "Strong", "troppo forte",
						"## Avvisi", "report-effetti-carte.csv");
		assertThat(csv.lines().toList()).hasSize(3);
		assertThat(csv.lines().findFirst().orElseThrow()).isEqualTo(EffectAblationReport.CSV_HEADER);
		assertThat(csv).contains("10,Strong,CREATURE,Wound,Uncommon,50,0.0800,0.0100,8.00,1.00,3.00,3.00,5.00,"
				+ "troppo forte");
	}

	private static EffectAblationReport report() {
		return new EffectAblationReport(BalanceGrid.PV30.configs().get(2), 1_250, BalanceRunner.BASE_SEED, 20_000);
	}

	private static EffectAblationReport calibrated() {
		EffectAblationReport report = report();
		report.addCalibration(vanilla(1), new Measure(100, 0.01, 0.002));
		return report;
	}

	private static Map<String, Row> rowsByName(EffectAblationReport report) {
		return report.rows().stream().collect(Collectors.toMap(row -> row.card().name(), Function.identity()));
	}

	private static CardDefinition vanilla(int cardId) {
		return TestCards.creature(cardId, "Vanilla " + cardId, WOUND, null, "Common", 5, 9, null);
	}

	private static CardDefinition creature(int cardId, String name) {
		return TestCards.creature(cardId, name, WOUND, null, "Uncommon", 4, 9, "CODE_" + cardId);
	}

	private static CardDefinition spell(int cardId, String name) {
		return TestCards.spell(cardId, name, CardSubtype.DAMAGE, "CODE_" + cardId);
	}
}
