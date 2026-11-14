package it.downfall.reversky.api.admin.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.ruleset.dto.RuleSetCreateRequest;
import it.downfall.reversky.api.ruleset.dto.RuleSetResponse;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link AdminRuleSetController} via HTTP.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class AdminRuleSetControllerIT {

	@Autowired private MockMvc mockMvc;
	@Autowired private RuleSetService ruleSetService;

	@MockitoBean private EmailSender emailSender;

	@Test
	@DisplayName("GET /admin/rule-sets as ADMIN returns the list")
	void findAll_asAdmin_returnsList() throws Exception {
		mockMvc.perform(get("/api/v1/admin/rule-sets")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("GET /admin/rule-sets as USER is forbidden")
	void findAll_asUser_isForbidden() throws Exception {
		mockMvc.perform(get("/api/v1/admin/rule-sets")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("POST /admin/rule-sets creates the rule set and returns 201")
	void create_validRequest_returns201() throws Exception {
		String body = """
			{
			  "version": "%s",
			  "startingLifePoints": 20,
			  "maxRounds": 10,
			  "deckSize": 30,
			  "deckCreatures": 15,
			  "deckSpells": 5,
			  "deckHidden": 4,
			  "deckItems": 3,
			  "deckTerrains": 3,
			  "maxHiddenSurvival": 2,
			  "maxHiddenDamage": 3,
			  "signupBonus": 100,
			  "winReward": 50,
			  "drawReward": 20
			}
			""".formatted(uniqueVersion());

		mockMvc.perform(post("/api/v1/admin/rule-sets")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.isActive").value(false));
	}

	@Test
	@DisplayName("POST /admin/rule-sets with invalid fields returns 400")
	void create_invalidFields_returns400() throws Exception {
		String body = """
			{ "version": "", "startingLifePoints": 0, "maxRounds": 0,
			  "deckSize": 0, "deckCreatures": 0, "deckSpells": 0, "deckHidden": 0,
			  "deckItems": 0, "deckTerrains": 0, "maxHiddenSurvival": 0,
			  "maxHiddenDamage": 0, "signupBonus": -1, "winReward": -1, "drawReward": -1 }
			""";

		mockMvc.perform(post("/api/v1/admin/rule-sets")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("POST /admin/rule-sets with mismatching deck parts returns 422")
	void create_deckPartsMismatch_returns422() throws Exception {
		String body = """
			{ "version": "%s", "startingLifePoints": 20, "maxRounds": 10,
			  "deckSize": 30, "deckCreatures": 15, "deckSpells": 5, "deckHidden": 4,
			  "deckItems": 3, "deckTerrains": 4, "maxHiddenSurvival": 2,
			  "maxHiddenDamage": 3, "signupBonus": 100, "winReward": 50, "drawReward": 20 }
			""".formatted(uniqueVersion());

		mockMvc.perform(post("/api/v1/admin/rule-sets")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("RULE_SET_RULES_VIOLATED"));
	}

	@Test
	@DisplayName("POST /admin/rule-sets with a duplicate version returns 409")
	void create_duplicateVersion_returns409() throws Exception {
		RuleSetResponse existing = ruleSetService.create(createRequest(uniqueVersion()), null);

		String body = """
			{ "version": "%s", "startingLifePoints": 20, "maxRounds": 10,
			  "deckSize": 30, "deckCreatures": 15, "deckSpells": 5, "deckHidden": 4,
			  "deckItems": 3, "deckTerrains": 3, "maxHiddenSurvival": 2,
			  "maxHiddenDamage": 3, "signupBonus": 100, "winReward": 50, "drawReward": 20 }
			""".formatted(existing.version());

		mockMvc.perform(post("/api/v1/admin/rule-sets")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("RULE_SET_VERSION_TAKEN"));
	}

	@Test
	@DisplayName("PUT /admin/rule-sets/{id} updates the rule set")
	void update_validRequest_returnsUpdated() throws Exception {
		RuleSetResponse created = ruleSetService.create(createRequest(uniqueVersion()), null);
		String newVersion = uniqueVersion();

		String body = """
			{ "version": "%s", "startingLifePoints": 25, "maxRounds": 15,
			  "deckSize": 32, "deckCreatures": 15, "deckSpells": 6, "deckHidden": 4,
			  "deckItems": 3, "deckTerrains": 4, "maxHiddenSurvival": 2,
			  "maxHiddenDamage": 3, "signupBonus": 120, "winReward": 60, "drawReward": 25 }
			""".formatted(newVersion);

		mockMvc.perform(put("/api/v1/admin/rule-sets/" + created.id())
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(newVersion))
			.andExpect(jsonPath("$.startingLifePoints").value(25));
	}

	@Test
	@DisplayName("PUT /admin/rule-sets/{id} with unknown id returns 404")
	void update_unknownId_returns404() throws Exception {
		String body = """
			{ "version": "%s", "startingLifePoints": 20, "maxRounds": 10,
			  "deckSize": 30, "deckCreatures": 15, "deckSpells": 5, "deckHidden": 4,
			  "deckItems": 3, "deckTerrains": 3, "maxHiddenSurvival": 2,
			  "maxHiddenDamage": 3, "signupBonus": 100, "winReward": 50, "drawReward": 20 }
			""".formatted(uniqueVersion());

		mockMvc.perform(put("/api/v1/admin/rule-sets/999999")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("RULE_SET_NOT_FOUND"));
	}

	@Test
	@DisplayName("POST /admin/rule-sets/{id}/activate deactivates the previous active one")
	void activate_secondRuleSet_deactivatesFirst() throws Exception {
		RuleSetResponse first = ruleSetService.create(createRequest(uniqueVersion()), null);
		RuleSetResponse second = ruleSetService.create(createRequest(uniqueVersion()), null);
		ruleSetService.activate(first.id());

		mockMvc.perform(post("/api/v1/admin/rule-sets/" + second.id() + "/activate")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(second.id()))
			.andExpect(jsonPath("$.isActive").value(true));
	}

	@Test
	@DisplayName("POST /admin/rule-sets/{id}/activate as USER is forbidden")
	void activate_asUser_isForbidden() throws Exception {
		RuleSetResponse created = ruleSetService.create(createRequest(uniqueVersion()), null);

		mockMvc.perform(post("/api/v1/admin/rule-sets/" + created.id() + "/activate")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
			.andExpect(status().isForbidden());
	}

	// --- supporto ---

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