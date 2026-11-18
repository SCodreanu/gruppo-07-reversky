package it.downfall.reversky.engine.web;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
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

	// TODO(@tommasofatticcioni): svuotare quando l'EffectRegistry controlla i codici effetto (ENG-22, #60)
	private static final Set<String> WAITING_FOR_EFFECT_REGISTRY = Set.of("90-errore-effetto-sconosciuto.json");

	@Autowired
	private MockMvc mockMvc;

	@ParameterizedTest(name = "{0}")
	@CsvFileSource(files = "examples/expected-results.csv", numLinesToSkip = 1)
	@DisplayName("Ogni esempio di examples/ dà il risultato calcolato a mano")
	void example_matchesExpectedResult(String file, int status, String result, String endReason,
			Integer totalRounds, Integer challengerLifePoints, Integer opponentLifePoints,
			Integer challengerDefeated, Integer opponentDefeated, String code) throws Exception {
		assumeFalse(WAITING_FOR_EFFECT_REGISTRY.contains(file),
				"UNKNOWN_EFFECT arriva con l'EffectRegistry (ENG-22, #60)");

		checkExample(file, status, result, endReason, totalRounds, challengerLifePoints, opponentLifePoints,
				challengerDefeated, opponentDefeated, code);
	}

	// TODO(@tommasofatticcioni): togliere @Disabled quando HIDEBEHIND è implementato (ENG-23, #61)
	@Disabled("Gli esempi con effetti (09, HIDEBEHIND) arrivano con gli effetti delle creature (ENG-23, #61)")
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
