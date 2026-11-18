package it.downfall.reversky.engine.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Test di accettazione: ogni richiesta di examples/ passa dal controller e dà il risultato calcolato a mano
 * (examples/README.md, docs/test "Test di accettazione con gli esempi").
 * <p>
 * Una riga di CSV è un test: se una regola cambia si aggiorna il CSV, non questo codice. Il percorso
 * {@code examples/...} è relativo alla cartella {@code reversky-engine}, la cartella di lavoro dei test.
 */
@WebMvcTest(SimulationController.class)
@Import({ EngineConfiguration.class, SimulationMapper.class })
class SimulationExamplesTest {

	@Autowired
	private MockMvc mockMvc;

	@ParameterizedTest(name = "{0}")
	@CsvFileSource(files = "examples/expected-results.csv", numLinesToSkip = 1)
	@DisplayName("Ogni esempio di examples/ dà il risultato calcolato a mano")
	void example_matchesExpectedResult(String file, int status, String result, String endReason,
			Integer totalRounds, Integer challengerLifePoints, Integer opponentLifePoints,
			Integer challengerDefeated, Integer opponentDefeated, String code) throws Exception {
		checkExample(file, status, result, endReason, totalRounds, challengerLifePoints, opponentLifePoints,
				challengerDefeated, opponentDefeated, code);
	}

	@ParameterizedTest(name = "{0}")
	@CsvFileSource(files = "examples/expected-results-effects.csv", numLinesToSkip = 1)
	@DisplayName("Ogni esempio con effetti di examples/ dà il risultato calcolato a mano")
	void effectExample_matchesExpectedResult(String file, int status, String result, String endReason,
			Integer totalRounds, Integer challengerLifePoints, Integer opponentLifePoints,
			Integer challengerDefeated, Integer opponentDefeated, String code) throws Exception {
		checkExample(file, status, result, endReason, totalRounds, challengerLifePoints, opponentLifePoints,
				challengerDefeated, opponentDefeated, code);
	}

	private void checkExample(String file, int status, String result, String endReason, Integer totalRounds,
			Integer challengerLifePoints, Integer opponentLifePoints, Integer challengerDefeated,
			Integer opponentDefeated, String code) throws Exception {
		String body = Files.readString(Path.of("examples", file));

		ResultActions response = mockMvc.perform(post("/api/v1/simulations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().is(status));

		if (status == 200) {
			response.andExpect(jsonPath("$.result").value(result))
					.andExpect(jsonPath("$.endReason").value(endReason))
					.andExpect(jsonPath("$.totalRounds").value(totalRounds))
					.andExpect(jsonPath("$.challenger.finalLifePoints").value(challengerLifePoints))
					.andExpect(jsonPath("$.opponent.finalLifePoints").value(opponentLifePoints))
					.andExpect(jsonPath("$.challenger.creaturesDefeated").value(challengerDefeated))
					.andExpect(jsonPath("$.opponent.creaturesDefeated").value(opponentDefeated));
		} else {
			response.andExpect(jsonPath("$.code").value(code));
		}
	}
}
