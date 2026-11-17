package it.downfall.reversky.engine.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Swagger dell'engine (D-41, docs/api-engine "Note"): ogni endpoint è descritto, con le risposte di errore.
 * Serve il contesto completo, perché springdoc legge tutti i controller.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

	private static final String SIMULATIONS = "$.paths['/api/v1/simulations'].post";

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("Il documento OpenAPI descrive POST /api/v1/simulations con le risposte 200, 400 e 500")
	void apiDocs_describeSimulationEndpoint() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Reversky Engine"))
				.andExpect(jsonPath(SIMULATIONS + ".summary").isNotEmpty())
				.andExpect(jsonPath(SIMULATIONS + ".requestBody.content['application/json']").exists())
				.andExpect(jsonPath(SIMULATIONS + ".responses['200']").exists())
				.andExpect(jsonPath(SIMULATIONS + ".responses['400']").exists())
				.andExpect(jsonPath(SIMULATIONS + ".responses['500']").exists())
				.andExpect(jsonPath("$.components.schemas.SimulationRequest").exists())
				.andExpect(jsonPath("$.components.schemas.SimulationResponse").exists());
	}

	@Test
	@DisplayName("Swagger UI risponde su /swagger-ui.html, come nell'API")
	void swaggerUi_isServed() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
				.andExpect(status().is3xxRedirection());
	}
}
