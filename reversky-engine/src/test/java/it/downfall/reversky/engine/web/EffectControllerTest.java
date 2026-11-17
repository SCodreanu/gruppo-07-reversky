package it.downfall.reversky.engine.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import it.downfall.reversky.engine.core.effect.EffectRegistry;

/**
 * Test di {@link EffectController} (docs/api-engine, {@code GET /api/v1/effects}): elenca il registro del gioco, per
 * tipo di carta e poi per codice.
 */
@WebMvcTest(EffectController.class)
@Import(EngineConfiguration.class)
class EffectControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EffectRegistry registry;

	@Test
	@DisplayName("200 con codice e tipo di carta di ogni effetto del registro, in JSON")
	void list_everyRegisteredEffect() throws Exception {
		// Act
		String body = mockMvc.perform(get("/api/v1/effects"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$[0].code").value("BITE_OF_REVERSE"))
				.andExpect(jsonPath("$[0].appliesTo").value("SPELL"))
				.andReturn().getResponse().getContentAsString();

		// Assert: stessi codici del registro, ognuno con il suo tipo
		List<String> codes = JsonPath.read(body, "$[*].code");
		assertThat(codes).containsExactlyInAnyOrderElementsOf(registry.codes().keySet());
		registry.codes().forEach((code, type) -> assertThat(JsonPath.<List<String>>read(body,
				"$[?(@.code == '" + code + "')].appliesTo")).containsExactly(type.name()));
	}

	@Test
	@DisplayName("Ordine stabile: per tipo di carta (SPELL, HIDDEN, TERRAIN, ITEM) e poi per codice")
	void list_sortedByTypeThenCode() throws Exception {
		// Act
		String body = mockMvc.perform(get("/api/v1/effects")).andReturn().getResponse().getContentAsString();

		// Assert
		List<String> codes = JsonPath.read(body, "$[*].code");
		assertThat(codes).containsExactly(
				"BITE_OF_REVERSE", "CRACK_CALL", "DISSOLUTION", "FORGOTTEN_ECHO", "RAIN_ARMOR", "REGISTRY_PACT",
				"REVERSE_FUSION", "SCP_458", "SKIN_FRACTURE", "VEIL_OF_ANCHORED",
				"BREAKING_POINT", "ECHO_STRIKE", "HOSTILE_PRESENCE", "LAST_SANCTUARY", "SURVIVAL_INSTINCT",
				"DEVOURING_ABYSS", "ECHO_RIFT", "HOLLOW_CLEARING", "WATCHING_RIFT",
				"DENIED_EXIT_BADGE", "FALSE_PATHS_COMPASS", "SEALED_FILE_ZERO", "TORII_BELL");
	}
}
