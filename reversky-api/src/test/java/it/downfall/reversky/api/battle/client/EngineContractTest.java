package it.downfall.reversky.api.battle.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.net.httpserver.HttpServer;

import it.downfall.reversky.api.battle.exception.EngineInputRejectedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Test di contratto con l'engine (ENG-24, docs/sfide "Test di contratto"): legge gli stessi file del test dell'engine,
 * le richieste di {@code reversky-engine/examples/} e le fotografie delle risposte in {@code examples/responses/}.
 * <p>
 * Per ogni esempio la richiesta passa da {@link SimulationRequest} e da {@link EngineClient} e deve arrivare al server
 * finto <b>identica</b> al file: un campo che l'API non conosce, non manda o manda con un altro nome fa fallire il test.
 * La fotografia della risposta, servita dal server finto con il suo status, deve essere letta da {@link EngineClient}
 * senza perdere niente (riscritta, è uguale al file) e dare i valori di {@code expected-results*.csv}. Il {@code log}
 * resta un JSON qualsiasi, salvato così com'è. Gli errori {@code 400} diventano {@code ENGINE_INPUT_REJECTED} (API-71).
 * <p>
 * Il test dell'engine ({@code SimulationContractTest}) controlla che le fotografie siano le sue risposte vere: insieme,
 * una modifica al contratto da una delle due parti fa fallire un test.
 */
class EngineContractTest {

	// La cartella di lavoro dei test è reversky-api: gli esempi sono dell'engine, nello stesso repository
	private static final Path EXAMPLES = Path.of("..", "reversky-engine", "examples");
	private static final Path RESPONSES = EXAMPLES.resolve("responses");
	private static final List<String> CSV_FILES = List.of("expected-results.csv", "expected-results-effects.csv");

	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	static Stream<String> examples() throws IOException {
		try (Stream<Path> files = Files.list(EXAMPLES)) {
			return files.map(path -> path.getFileName().toString())
				.filter(name -> name.endsWith(".json"))
				.sorted()
				.toList()
				.stream();
		}
	}

	@Test
	@DisplayName("Every example has a response snapshot and a row in the expected results")
	void everyExample_hasSnapshotAndExpectedRow() throws IOException {
		Map<String, Expected> expected = expectedResults();

		assertThat(examples().toList())
			.isNotEmpty()
			.allSatisfy(file -> {
				assertThat(RESPONSES.resolve(file)).exists();
				assertThat(expected).containsKey(file);
			});
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("examples")
	@DisplayName("The request reaches the engine exactly as in the example file")
	void request_roundTripsExactly(String file) throws IOException {
		// Arrange
		String json = Files.readString(EXAMPLES.resolve(file));
		SimulationRequest request = MAPPER.readValue(json, SimulationRequest.class);
		Expected expected = expectedResults().get(file);
		AtomicReference<String> received = new AtomicReference<>();
		EngineClient client = clientFor(expected.status(), Files.readString(RESPONSES.resolve(file)), received);

		// Act
		try {
			client.simulate(request);
		} catch (EngineInputRejectedException e) {
			// Gli esempi di errore: qui conta solo la richiesta, la risposta la controlla l'altro test
		}

		// Assert
		assertThat(MAPPER.readTree(received.get())).isEqualTo(MAPPER.readTree(json));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("examples")
	@DisplayName("The engine response snapshot is read without losing anything and gives the expected result")
	void response_isReadCompletely(String file) throws IOException {
		// Arrange
		SimulationRequest request = MAPPER.readValue(Files.readString(EXAMPLES.resolve(file)), SimulationRequest.class);
		String snapshot = Files.readString(RESPONSES.resolve(file));
		Expected expected = expectedResults().get(file);
		EngineClient client = clientFor(expected.status(), snapshot, new AtomicReference<>());

		// Act + Assert
		if (expected.status() != 200) {
			assertThatThrownBy(() -> client.simulate(request)).isInstanceOf(EngineInputRejectedException.class);
			assertThat(MAPPER.readValue(snapshot, EngineProblem.class).code()).isEqualTo(expected.code());
			return;
		}
		SimulationResponse response = client.simulate(request);
		assertThat(MAPPER.readTree(MAPPER.writeValueAsString(response))).isEqualTo(MAPPER.readTree(snapshot));
		assertThat(response.result()).isEqualTo(expected.result());
		assertThat(response.endReason()).isEqualTo(expected.endReason());
		assertThat(response.totalRounds()).isEqualTo(expected.totalRounds());
		assertThat(response.challenger().finalLifePoints()).isEqualTo(expected.challengerLifePoints());
		assertThat(response.opponent().finalLifePoints()).isEqualTo(expected.opponentLifePoints());
		assertThat(response.challenger().creaturesDefeated()).isEqualTo(expected.challengerDefeated());
		assertThat(response.opponent().creaturesDefeated()).isEqualTo(expected.opponentDefeated());
		assertThat(response.seed()).isEqualTo(request.seed());
		assertThat(response.rulesVersion()).isEqualTo(request.rules().rulesVersion());
	}

	// Un server HTTP del JDK che risponde alla simulazione con quel corpo e quello status, come EngineClientTest
	private EngineClient clientFor(int status, String body, AtomicReference<String> received) throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/api/v1/simulations", exchange -> {
			received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type",
				status == 200 ? "application/json" : "application/problem+json");
			exchange.sendResponseHeaders(status, bytes.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		});
		server.start();
		return new EngineClient(new EngineProperties("http://localhost:" + server.getAddress().getPort(),
			Duration.ofSeconds(5)));
	}

	// Le righe dei due CSV dell'engine, per nome di file; i campi vuoti delle righe di errore restano null
	private static Map<String, Expected> expectedResults() throws IOException {
		Map<String, Expected> rows = new HashMap<>();
		for (String csv : CSV_FILES) {
			List<String> lines = Files.readAllLines(EXAMPLES.resolve(csv));
			for (String line : lines.subList(1, lines.size())) {
				if (line.isBlank()) {
					continue;
				}
				String[] cells = line.split(",", -1);
				rows.put(cells[0], new Expected(Integer.parseInt(cells[1]), text(cells[2]), text(cells[3]),
					number(cells[4]), number(cells[5]), number(cells[6]), number(cells[7]), number(cells[8]),
					text(cells[9])));
			}
		}
		return rows;
	}

	private static String text(String cell) {
		return cell.isBlank() ? null : cell.trim();
	}

	private static Integer number(String cell) {
		return cell.isBlank() ? null : Integer.valueOf(cell.trim());
	}

	/** Una riga di expected-results*.csv: le stesse colonne lette da SimulationExamplesTest dell'engine. */
	private record Expected(int status, String result, String endReason, Integer totalRounds,
			Integer challengerLifePoints, Integer opponentLifePoints, Integer challengerDefeated,
			Integer opponentDefeated, String code) {
	}
}
