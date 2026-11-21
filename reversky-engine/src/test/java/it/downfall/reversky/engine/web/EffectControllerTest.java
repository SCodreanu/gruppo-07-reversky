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
import it.downfall.reversky.engine.core.model.CardType;

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
				.andExpect(jsonPath("$[0].code").value("BAKU"))
				.andExpect(jsonPath("$[0].appliesTo").value("CREATURE"))
				.andReturn().getResponse().getContentAsString();

		// Assert: stessi codici del registro, ognuno con il suo tipo
		List<String> codes = JsonPath.read(body, "$[*].code");
		assertThat(codes).containsExactlyInAnyOrderElementsOf(registry.codes().keySet());
		registry.codes().forEach((code, type) -> assertThat(JsonPath.<List<String>>read(body,
				"$[?(@.code == '" + code + "')].appliesTo")).containsExactly(type.name()));
	}

	@Test
	@DisplayName("Ordine stabile: per tipo di carta (CREATURE, SPELL, HIDDEN, TERRAIN, ITEM) e poi per codice")
	void list_sortedByTypeThenCode() throws Exception {
		// Act
		String body = mockMvc.perform(get("/api/v1/effects")).andReturn().getResponse().getContentAsString();

		// Assert: ogni coppia (tipo, codice) viene dopo la precedente; i tipi nell'ordine di CardType
		List<String> codes = JsonPath.read(body, "$[*].code");
		List<String> types = JsonPath.read(body, "$[*].appliesTo");
		assertThat(types).first().isEqualTo("CREATURE");
		assertThat(types).last().isEqualTo("ITEM");
		for (int i = 1; i < codes.size(); i++) {
			int byType = Integer.compare(CardType.valueOf(types.get(i - 1)).ordinal(),
					CardType.valueOf(types.get(i)).ordinal());
			assertThat(byType < 0 || byType == 0 && codes.get(i - 1).compareTo(codes.get(i)) < 0)
					.as("%s (%s) prima di %s (%s)", codes.get(i - 1), types.get(i - 1), codes.get(i), types.get(i))
					.isTrue();
		}
	}
}
