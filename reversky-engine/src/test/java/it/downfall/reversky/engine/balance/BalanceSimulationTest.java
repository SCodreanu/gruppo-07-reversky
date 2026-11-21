package it.downfall.reversky.engine.balance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import it.downfall.reversky.engine.core.model.CardDefinition;

/**
 * Simulazione di bilanciamento ({@code ENG-28}): le configurazioni di una griglia × 16 accoppiamenti di fazioni × 2.500
 * partite, poi il report in {@code docs/bilanciamento/}. La griglia base ({@code report-bilanciamento.md} e
 * {@code report-carte.csv}) si cambia con la proprietà di sistema {@value #GRID_PROPERTY} ({@link BalanceGrid}).
 * <p>
 * È lenta: ha il tag {@code balance}, è esclusa da {@code gradlew build} e si lancia con {@code gradlew balanceTest}.
 * Controlla solo le invarianti (nessun errore dell'engine, ogni partita finisce, le prime 200 partite di ogni
 * configurazione danno lo stesso risultato due volte); le soglie di bilanciamento sono avvisi nel report, non
 * fallimenti (deciso il 20 novembre). La cartella del report si cambia con la proprietà di sistema
 * {@value #OUTPUT_DIR_PROPERTY}.
 */
@Tag("balance")
class BalanceSimulationTest {

	/** Proprietà di sistema con la cartella del report; senza, {@code docs/bilanciamento}. */
	static final String OUTPUT_DIR_PROPERTY = "balance.outputDir";

	/** Proprietà di sistema con il nome della griglia ({@link BalanceGrid}); senza, la griglia base. */
	static final String GRID_PROPERTY = "balance.grid";

	private static final int MATCHES_PER_PAIRING = 2_500;
	private static final int DETERMINISM_SAMPLE = 200;
	private static final Path DEFAULT_OUTPUT_DIR = Path.of("docs", "bilanciamento");

	@Test
	@Timeout(value = 3, unit = TimeUnit.HOURS)
	@DisplayName("Bilanciamento: tutte le partite finiscono, deterministiche; report scritto in docs/bilanciamento")
	void simulateGridAndWriteReport() throws IOException {
		List<CardDefinition> catalog = CatalogCsv.load(CatalogCsv.DEFAULT_PATH);
		BalanceRunner runner = new BalanceRunner(catalog, MATCHES_PER_PAIRING, BalanceRunner.BASE_SEED);
		BalanceGrid grid = BalanceGrid.byName(System.getProperty(GRID_PROPERTY, "base"));
		BalanceReport report = new BalanceReport(catalog, MATCHES_PER_PAIRING, BalanceRunner.BASE_SEED, grid);

		for (BalanceConfig config : grid.configs()) {
			BalanceStats stats = runner.run(config, DETERMINISM_SAMPLE);
			assertThat(stats.matches()).isEqualTo(BalancePairing.all().size() * MATCHES_PER_PAIRING);
			report.add(config, stats);
			System.out.println("Bilanciamento: " + config.id() + " completata, " + stats.matches() + " partite");
		}

		Path dir = Path.of(System.getProperty(OUTPUT_DIR_PROPERTY, DEFAULT_OUTPUT_DIR.toString()));
		Files.createDirectories(dir);
		Files.writeString(dir.resolve(grid.reportFile()), report.markdown(), StandardCharsets.UTF_8);
		Files.writeString(dir.resolve(grid.csvFile()), report.csv(), StandardCharsets.UTF_8);
	}
}
