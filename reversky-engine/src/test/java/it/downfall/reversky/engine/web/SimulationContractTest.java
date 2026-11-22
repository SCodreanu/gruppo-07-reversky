package it.downfall.reversky.engine.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Test di contratto con l'API (ENG-24, docs/test "Test di contratto"): la risposta di ogni richiesta di
 * {@code examples/} deve essere uguale alla sua fotografia in {@code examples/responses/}, log compreso.
 * <p>
 * Le stesse fotografie le legge il test di contratto dell'API attraverso il suo {@code EngineClient}: se una risposta
 * cambia, fallisce prima questo test. Il confronto è tra alberi JSON (ordine dei campi e spazi non contano).
 * <p>
 * Quando una risposta cambia di proposito, il test scrive quella vera in {@code build/contract/actual/} e la nomina nel
 * messaggio: si controlla e la si copia a mano in {@code examples/responses/} (deciso il 22 novembre). Una fotografia
 * non cambia mai da sola.
 */
@WebMvcTest(SimulationController.class)
@Import({ EngineConfiguration.class, SimulationMapper.class })
class SimulationContractTest {

	private static final Path EXAMPLES = Path.of("examples");
	private static final Path RESPONSES = EXAMPLES.resolve("responses");
	private static final Path ACTUAL = Path.of("build", "contract", "actual");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * Tutte le richieste di {@code examples/}, anche quelle di errore: il contratto comprende i {@code ProblemDetail}.
	 */
	static Stream<String> examples() throws IOException {
		try (Stream<Path> files = Files.list(EXAMPLES)) {
			return files.map(path -> path.getFileName().toString())
					.filter(name -> name.endsWith(".json"))
					.sorted()
					.toList()
					.stream();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("examples")
	@DisplayName("Ogni esempio dà esattamente la risposta della sua fotografia in examples/responses/")
	void example_matchesResponseSnapshot(String file) throws Exception {
		// Arrange
		String request = Files.readString(EXAMPLES.resolve(file));
		Path snapshot = RESPONSES.resolve(file);

		// Act
		String body = mockMvc.perform(post("/api/v1/simulations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andReturn().getResponse().getContentAsString();

		// Assert
		JsonNode actual = objectMapper.readTree(body);
		JsonNode expected = Files.exists(snapshot) ? objectMapper.readTree(Files.readString(snapshot)) : null;
		if (!actual.equals(expected)) {
			Path written = writeActual(file, actual);
			assertThat(actual)
					.as("The response of %s differs from %s; the real one is in %s: check it and copy it there",
							file, snapshot, written)
					.isEqualTo(expected);
		}
	}

	// La risposta vera, indentata come le richieste, per controllarla e copiarla nella fotografia
	private Path writeActual(String file, JsonNode actual) throws IOException {
		Files.createDirectories(ACTUAL);
		Path written = ACTUAL.resolve(file);
		Files.writeString(written, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual) + "\n");
		return written;
	}
}
