package it.downfall.reversky.engine.balance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import it.downfall.reversky.engine.balance.EffectAblation.Measure;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardType;

/**
 * Valore di ogni effetto del catalogo a partite appaiate ({@code ENG-42}, fase 2, scelto il 21 novembre): 30 PV con
 * +1 ATK contro +1 DEF, 16 accoppiamenti × 1.250 partite di base; poi la taratura con +1 DEF sulle creature senza
 * effetto e una misura per ogni carta con effetto. Scrive {@value EffectAblationReport#REPORT_FILE} e
 * {@value EffectAblationReport#CSV_FILE} in {@code docs/bilanciamento/}.
 * <p>
 * È lenta: ha il tag {@code effects}, è esclusa da {@code gradlew build} e si lancia con {@code gradlew effectTest}.
 * Controlla solo le invarianti (nessun errore dell'engine, ogni partita finisce, le prime 200 partite danno lo stesso
 * risultato due volte); i giudizi sono nel report. La cartella si cambia con la proprietà di sistema
 * {@value BalanceSimulationTest#OUTPUT_DIR_PROPERTY}.
 */
@Tag("effects")
class EffectAblationSimulationTest {

	private static final int MATCHES_PER_PAIRING = 1_250;
	private static final int DETERMINISM_SAMPLE = 200;
	private static final Path DEFAULT_OUTPUT_DIR = Path.of("docs", "bilanciamento");

	@Test
	@Timeout(value = 3, unit = TimeUnit.HOURS)
	@DisplayName("Valore degli effetti: tutte le partite finiscono, deterministiche; report scritto")
	void measureEveryEffectAndWriteReport() throws IOException {
		List<CardDefinition> catalog = CatalogCsv.load(CatalogCsv.DEFAULT_PATH);
		Map<Integer, Double> prices = CatalogCsv.effectPrices(CatalogCsv.DEFAULT_PATH);
		BalanceRunner runner = new BalanceRunner(catalog, MATCHES_PER_PAIRING, BalanceRunner.BASE_SEED,
				EffectAblation.registry());
		// 30 PV, +1 ATK contro +1 DEF: la proposta della fase 1
		BalanceConfig config = BalanceGrid.PV30.configs().get(2);
		EffectAblation ablation = new EffectAblation(runner, config, DETERMINISM_SAMPLE);
		assertThat(ablation.matches()).isEqualTo(BalancePairing.all().size() * MATCHES_PER_PAIRING);
		EffectAblationReport report = new EffectAblationReport(config, MATCHES_PER_PAIRING, BalanceRunner.BASE_SEED,
				ablation.matches());
		System.out.println("Valore degli effetti: " + ablation.matches() + " partite di base");

		for (CardDefinition card : catalog) {
			if (card.type() == CardType.CREATURE && card.effectCode() == null) {
				Measure measure = ablation.measure(card, EffectAblation::withExtraDefense);
				report.addCalibration(card, measure);
				System.out.println("Taratura: " + card.name() + ", " + measure.matches() + " partite");
			}
		}
		for (CardDefinition card : catalog) {
			if (card.effectCode() != null) {
				Measure measure = ablation.measure(card, EffectAblation::withoutEffect).negated();
				report.addEffect(card, prices.get(card.cardId()), measure);
				System.out.println("Effetto: " + card.name() + ", " + measure.matches() + " partite");
			}
		}

		String outputDir = System.getProperty(BalanceSimulationTest.OUTPUT_DIR_PROPERTY, DEFAULT_OUTPUT_DIR.toString());
		Path dir = Path.of(outputDir);
		Files.createDirectories(dir);
		Files.writeString(dir.resolve(EffectAblationReport.REPORT_FILE), report.markdown(), StandardCharsets.UTF_8);
		Files.writeString(dir.resolve(EffectAblationReport.CSV_FILE), report.csv(), StandardCharsets.UTF_8);
	}
}
