package it.downfall.reversky.engine.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import tools.jackson.databind.ObjectMapper;

/**
 * Swagger dell'engine (D-41, docs/api-engine "Note"): ogni endpoint è descritto, con le risposte di errore, e gli
 * esempi di richiesta danno una partita vera. Serve il contesto completo, perché springdoc legge tutti i controller.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

	private static final String SIMULATIONS = "$.paths['/api/v1/simulations'].post";
	private static final String REQUEST_EXAMPLES = SIMULATIONS + ".requestBody.content['application/json'].examples";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("Il documento OpenAPI descrive POST /api/v1/simulations: 200 in JSON, 400 e 500 come ProblemDetail")
	void apiDocs_describeSimulationEndpoint() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Reversky Engine"))
				.andExpect(jsonPath(SIMULATIONS + ".summary").isNotEmpty())
				.andExpect(jsonPath(SIMULATIONS + ".requestBody.content['application/json'].schema['$ref']")
						.value("#/components/schemas/SimulationRequest"))
				.andExpect(jsonPath(SIMULATIONS + ".responses['200'].content['application/json'].schema['$ref']")
						.value("#/components/schemas/SimulationResponse"))
				.andExpect(jsonPath(SIMULATIONS + ".responses['200'].content['*/*']").doesNotExist())
				.andExpect(jsonPath(SIMULATIONS + ".responses['400'].content['application/problem+json'].schema['$ref']")
						.value("#/components/schemas/ProblemDetail"))
				.andExpect(jsonPath(SIMULATIONS + ".responses['400'].content['*/*']").doesNotExist())
				.andExpect(jsonPath(SIMULATIONS + ".responses['500'].content['application/problem+json'].schema['$ref']")
						.value("#/components/schemas/ProblemDetail"))
				.andExpect(jsonPath(SIMULATIONS + ".responses['500'].content['*/*']").doesNotExist());
	}

	@Test
	@DisplayName("Il documento OpenAPI descrive GET /api/v1/effects: 200 con un array di EffectResponse in JSON")
	void apiDocs_describeEffectsEndpoint() throws Exception {
		String effects = "$.paths['/api/v1/effects'].get";
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath(effects + ".summary").isNotEmpty())
				.andExpect(jsonPath(effects + ".responses['200'].content['application/json'].schema.type")
						.value("array"))
				.andExpect(jsonPath(effects + ".responses['200'].content['application/json'].schema.items['$ref']")
						.value("#/components/schemas/EffectResponse"))
				.andExpect(jsonPath(effects + ".responses['200'].content['*/*']").doesNotExist());
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource({
			"basic, examples/01-base-esempio-regolamento.json, CHALLENGER_WIN, 1",
			"full, examples/08-partita-completa-seed-42.json, CHALLENGER_WIN, 14" })
	@DisplayName("Ogni esempio di Swagger è uguale al suo file di examples/ e dà la partita calcolata a mano")
	void requestExample_isTheExampleFileAndPlaysTheMatch(String name, String file, String result, int totalRounds)
			throws Exception {
		// Arrange: l'esempio come lo mostra Swagger UI
		String docs = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
		Map<String, Object> example = JsonPath.read(docs, REQUEST_EXAMPLES + "." + name);
		String body = objectMapper.writeValueAsString(example.get("value"));

		// Assert: stesso JSON del file, e la partita si gioca
		assertThat(objectMapper.readTree(body)).isEqualTo(objectMapper.readTree(Files.readString(Path.of(file))));
		mockMvc.perform(post("/api/v1/simulations").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result").value(result))
				.andExpect(jsonPath("$.totalRounds").value(totalRounds));
	}

	@Test
	@DisplayName("Swagger UI risponde su /swagger-ui.html, come nell'API")
	void swaggerUi_isServed() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
				.andExpect(status().is3xxRedirection());
	}
}
