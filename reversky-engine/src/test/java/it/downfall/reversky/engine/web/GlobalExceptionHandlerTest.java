package it.downfall.reversky.engine.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import it.downfall.reversky.engine.core.simulation.MatchSimulator;
import it.downfall.reversky.engine.core.validation.ErrorCode;
import it.downfall.reversky.engine.core.validation.InvalidSimulationInputException;

/**
 * Test di {@link GlobalExceptionHandler}: ogni errore diventa un {@code ProblemDetail} con {@code code}
 * (docs/api-engine, "Errori"), come nell'API.
 * <p>
 * Il simulatore è finto: così si provocano sia un errore di input del core sia un bug.
 */
@WebMvcTest(SimulationController.class)
@Import(SimulationMapper.class)
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

	private static final String URL = "/api/v1/simulations";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MatchSimulator simulator;

	@Test
	@DisplayName("Un errore di input del core diventa 400 con il suo code e il suo messaggio")
	void invalidInput_returns400WithCoreCode() throws Exception {
		when(simulator.simulate(any())).thenThrow(
				new InvalidSimulationInputException(ErrorCode.UNKNOWN_EFFECT, "Unknown effect VEIL_OF_ANCHORD on card 96"));

		perform(SimulationControllerTest.VALID_REQUEST)
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Invalid simulation input"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("Unknown effect VEIL_OF_ANCHORD on card 96"))
				.andExpect(jsonPath("$.code").value("UNKNOWN_EFFECT"))
				.andExpect(jsonPath("$.instance").value(URL));
	}

	@Test
	@DisplayName("Un campo non valido diventa 400 VALIDATION_FAILED con l'elenco dei campi")
	void invalidField_returns400WithFieldList() throws Exception {
		String withoutSeedAndName = SimulationControllerTest.VALID_REQUEST
				.replace("\"seed\": 8812734650912,", "")
				.replace("\"name\": \"SCP-131\", ", "");

		perform(withoutSeedAndName)
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Invalid simulation input"))
				.andExpect(jsonPath("$.detail").value("One or more fields are invalid"))
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.instance").value(URL))
				.andExpect(jsonPath("$.errors[*].field").value(hasItem("seed")))
				.andExpect(jsonPath("$.errors[*].message").value(hasItem("Seed is required")))
				.andExpect(jsonPath("$.errors[*].field").value(hasItem("challenger.deck[0].name")))
				.andExpect(jsonPath("$.errors[*].message").value(hasItem("Card name is required")));
	}

	@Test
	@DisplayName("Un JSON malformato o con un valore di enum che non esiste diventa 400 VALIDATION_FAILED")
	void unreadableJson_returns400ValidationFailed() throws Exception {
		String unknownType = SimulationControllerTest.VALID_REQUEST.replace("\"type\": \"CREATURE\", \"rarity\": "
				+ "\"Common\"", "\"type\": \"MONSTER\", \"rarity\": \"Common\"");

		perform("{ \"seed\": ").andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.detail").value("Malformed JSON or a value of the wrong type"))
				.andExpect(jsonPath("$.instance").value(URL));
		perform(unknownType).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	@DisplayName("Un bug del motore diventa 500 INTERNAL_ERROR senza dettagli, e il seed finisce nel log tecnico")
	void bug_returns500AndLogsSeed(CapturedOutput output) throws Exception {
		when(simulator.simulate(any())).thenThrow(new IllegalStateException("CHALLENGER non ha una creatura"));

		perform(SimulationControllerTest.VALID_REQUEST)
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Internal error"))
				.andExpect(jsonPath("$.detail").value(not(containsString("creatura"))))
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.instance").value(URL));
		assertThat(output).contains("seed 8812734650912").contains("CHALLENGER non ha una creatura");
	}

	private ResultActions perform(String body) throws Exception {
		return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
	}
}
