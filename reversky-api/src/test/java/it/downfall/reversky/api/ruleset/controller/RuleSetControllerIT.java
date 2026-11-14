package it.downfall.reversky.api.ruleset.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.ruleset.dto.RuleSetCreateRequest;
import it.downfall.reversky.api.ruleset.dto.RuleSetResponse;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link RuleSetController} via HTTP: GET /rules senza token.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class RuleSetControllerIT {

	@Autowired private MockMvc mockMvc;
	@Autowired private RuleSetService ruleSetService;

	@MockitoBean private EmailSender emailSender;

	@Test
	@DisplayName("GET /rules is public: no token needed, returns the active rule set")
	void findActive_withoutToken_returnsActive() throws Exception {
		RuleSetResponse created = ruleSetService.create(createRequest(uniqueVersion()), null);
		ruleSetService.activate(created.id());

		mockMvc.perform(get("/api/v1/rules"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(created.version()))
			.andExpect(jsonPath("$.startingLifePoints").value(20))
			.andExpect(jsonPath("$.deckCreatures").value(15));
	}

	@Test
	@DisplayName("OpenAPI does not ask for a token on GET /rules")
	void apiDocs_rules_hasNoSecurity() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/rules'].get").exists())
			.andExpect(jsonPath("$.paths['/api/v1/rules'].get.security").doesNotExist());
	}

	private static String uniqueVersion() {
		return "v" + UUID.randomUUID().toString().substring(0, 8);
	}

	private static RuleSetCreateRequest createRequest(String version) {
		return new RuleSetCreateRequest(
			version, 20, 10,
			30, 15, 5, 4, 3, 3,
			2, 3, 100, 50, 20);
	}
}