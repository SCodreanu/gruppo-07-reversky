package it.downfall.reversky.engine.web;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Test di {@link SimulationController}: JSON del contratto (docs/api-engine), validazione dei DTO e status HTTP.
 * <p>
 * La partita è l'esempio 01 di examples/ con un seed lungo: SCP-131 (8/6) batte Eternal Cup (4/5) in un round.
 */
@WebMvcTest(SimulationController.class)
@Import({ EngineConfiguration.class, SimulationMapper.class })
class SimulationControllerTest {

	private static final String URL = "/api/v1/simulations";

	static final String VALID_REQUEST = """
			{
			  "seed": 8812734650912,
			  "rules": { "rulesVersion": "1.0", "startingLifePoints": 30, "maxRounds": 75 },
			  "factions": [
			    { "id": 1, "name": "Theory of the Wound" },
			    { "id": 3, "name": "Theory of the Archive" }
			  ],
			  "factionBonuses": [
			    { "originFactionId": 3, "targetFactionId": 1, "atkBonus": 2, "defBonus": 0 }
			  ],
			  "challenger": {
			    "userId": 7,
			    "deck": [
			      { "cardId": 301, "name": "SCP-131", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 3, "secondaryFactionId": null, "atk": 8, "def": 6, "subtype": null, "effectCode": null }
			    ]
			  },
			  "opponent": {
			    "userId": 12,
			    "deck": [
			      { "cardId": 305, "name": "Eternal Cup", "type": "CREATURE", "rarity": "Uncommon", "rarityOrder": 2,
			        "factionId": 3, "secondaryFactionId": null, "atk": 4, "def": 5, "subtype": null, "effectCode": null }
			    ]
			  }
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("Una richiesta valida restituisce 200 con il risultato e il log round per round")
	void simulate_validRequest_returnsResultAndLog() throws Exception {
		perform(VALID_REQUEST)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result").value("CHALLENGER_WIN"))
				.andExpect(jsonPath("$.endReason").value("NO_CREATURES_LEFT"))
				.andExpect(jsonPath("$.totalRounds").value(1))
				.andExpect(jsonPath("$.challenger.userId").value(7))
				.andExpect(jsonPath("$.challenger.finalLifePoints").value(30))
				.andExpect(jsonPath("$.challenger.creaturesDefeated").value(1))
				.andExpect(jsonPath("$.opponent.userId").value(12))
				.andExpect(jsonPath("$.opponent.finalLifePoints").value(27))
				.andExpect(jsonPath("$.opponent.creaturesDefeated").value(0))
				.andExpect(jsonPath("$.seed").value(8812734650912L))
				.andExpect(jsonPath("$.rulesVersion").value("1.0"))
				.andExpect(jsonPath("$.log[0].sequence").value(1))
				.andExpect(jsonPath("$.log[0].round").value(0))
				.andExpect(jsonPath("$.log[0].type").value("MATCH_STARTED"))
				.andExpect(jsonPath("$.log[0].side").value(nullValue()))
				.andExpect(jsonPath("$.log[0].data.startingLifePoints").value(30))
				.andExpect(jsonPath("$.log[0].message").value("Match started — 30 life points each"))
				.andExpect(jsonPath("$.log[-2].type").value("PLAYER_DAMAGED"))
				.andExpect(jsonPath("$.log[-2].side").value("OPPONENT"))
				.andExpect(jsonPath("$.log[-1].type").value("MATCH_ENDED"));
	}

	@Test
	@DisplayName("Un mazzo senza creature è rifiutato dal core con 400 NO_CREATURES_IN_DECK")
	void simulate_deckWithoutCreatures_returns400() throws Exception {
		String spellOnly = VALID_REQUEST
				.replace("\"name\": \"Eternal Cup\", \"type\": \"CREATURE\"", "\"name\": \"Eternal Cup\", \"type\": \"SPELL\"")
				.replace("\"factionId\": 3, \"secondaryFactionId\": null, \"atk\": 4",
						"\"factionId\": null, \"secondaryFactionId\": null, \"atk\": 4");

		perform(spellOnly)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("NO_CREATURES_IN_DECK"))
				.andExpect(jsonPath("$.detail").value("Deck without creatures: opponent"));
	}

	// TODO(@tommasofatticcioni): togliere @Disabled quando l'EffectRegistry controlla i codici (ENG-22, #60)
	@Disabled("UNKNOWN_EFFECT arriva con l'EffectRegistry (ENG-22, #60): oggi l'effetto si ignora e la partita si gioca")
	@Test
	@DisplayName("Un effectCode sconosciuto è rifiutato con 400 UNKNOWN_EFFECT")
	void simulate_unknownEffect_returns400() throws Exception {
		String unknownEffect = VALID_REQUEST.replace("\"atk\": 8, \"def\": 6, \"subtype\": null, \"effectCode\": null",
				"\"atk\": 8, \"def\": 6, \"subtype\": null, \"effectCode\": \"HIDEBEHND\"");

		perform(unknownEffect)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("UNKNOWN_EFFECT"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidRequests")
	@DisplayName("Un campo obbligatorio mancante o non valido è rifiutato con 400 VALIDATION_FAILED")
	void simulate_invalidRequest_returns400(String description, String invalidRequest) throws Exception {
		perform(invalidRequest)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	/**
	 * Richieste non valide per il controllo 1 (VALIDATION_FAILED) di docs/api-engine: ognuna cambia una sola cosa di
	 * {@link #VALID_REQUEST}.
	 */
	static Stream<Arguments> invalidRequests() {
		return Stream.of(
				invalid("seed mancante", "\"seed\": 8812734650912,", ""),
				invalid("rules mancante", "\"rules\": { \"rulesVersion\": \"1.0\", \"startingLifePoints\": 30, "
						+ "\"maxRounds\": 75 },", ""),
				invalid("rulesVersion vuota", "\"rulesVersion\": \"1.0\"", "\"rulesVersion\": \" \""),
				invalid("startingLifePoints mancante", "\"startingLifePoints\": 30, ", ""),
				invalid("maxRounds 0 (esempio 95)", "\"maxRounds\": 75", "\"maxRounds\": 0"),
				invalid("factions mancante", "\"factions\": [", "\"ignored\": ["),
				invalid("nome della fazione vuoto", "\"name\": \"Theory of the Wound\"", "\"name\": \"\""),
				invalid("factionBonuses mancante", "\"factionBonuses\": [", "\"ignored\": ["),
				invalid("defBonus mancante", ", \"defBonus\": 0 }", " }"),
				invalid("userId mancante", "\"userId\": 7,", ""),
				invalid("mazzo vuoto", "\"deck\": [\n      { \"cardId\": 301", "\"deck\": [], \"ignored\": [\n"
						+ "      { \"cardId\": 301"),
				invalid("cardId mancante", "\"cardId\": 301, ", ""),
				invalid("nome della carta mancante", "\"name\": \"SCP-131\", ", ""),
				invalid("rarità vuota", "\"rarity\": \"Uncommon\"", "\"rarity\": \"\""),
				invalid("rarityOrder mancante", "\"rarityOrder\": 2,", ""),
				invalid("tipo mancante", "\"type\": \"CREATURE\", \"rarity\": \"Common\"", "\"rarity\": \"Common\""),
				invalid("tipo che non esiste", "\"type\": \"CREATURE\", \"rarity\": \"Common\"",
						"\"type\": \"MONSTER\", \"rarity\": \"Common\""),
				invalid("sottotipo che non esiste", "\"atk\": 8, \"def\": 6, \"subtype\": null",
						"\"atk\": 8, \"def\": 6, \"subtype\": \"FOO\""),
				invalid("JSON malformato", "\"seed\": 8812734650912,", "\"seed\": 8812734650912"));
	}

	private static Arguments invalid(String description, String target, String replacement) {
		if (!VALID_REQUEST.contains(target)) {
			throw new IllegalArgumentException("Testo non trovato nella richiesta valida: " + target);
		}
		return Arguments.of(description, VALID_REQUEST.replace(target, replacement));
	}

	private ResultActions perform(String body) throws Exception {
		return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
	}
}
