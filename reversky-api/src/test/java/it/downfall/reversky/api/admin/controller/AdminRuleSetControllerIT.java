package it.downfall.reversky.api.admin.controller;

import static it.downfall.reversky.api.support.TestRuleSets.request;
import static it.downfall.reversky.api.support.TestRuleSets.requestWithDeck;
import static it.downfall.reversky.api.support.TestRuleSets.toJson;
import static it.downfall.reversky.api.support.TestRuleSets.uniqueVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.ruleset.dto.RuleSetRequest;
import it.downfall.reversky.api.ruleset.dto.RuleSetResponse;
import it.downfall.reversky.api.ruleset.entity.RuleSet;
import it.downfall.reversky.api.ruleset.repository.RuleSetRepository;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.support.TestRuleSets;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link AdminRuleSetController} via HTTP, con i filtri di Spring Security attivi.
 * <p>
 * Si verificano accesso (401, 403), validazione, status e formato degli errori. I casi limite
 * delle regole (ordine dei controlli, stessa versione sullo stesso rule set) sono in RuleSetServiceIT.
 * Il rule set attivo prima del test si rimette attivo dopo (vedi {@link TestRuleSets}).
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class AdminRuleSetControllerIT {

	private static final String BASE_PATH = "/api/v1/admin/rule-sets";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private RuleSetService ruleSetService;

	@Autowired
	private RuleSetRepository ruleSetRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// Non serve al test: stessa configurazione degli altri ControllerIT, così Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	private TestRuleSets testRuleSets;

	@BeforeEach
	void setUp() {
		testRuleSets = new TestRuleSets(ruleSetService, ruleSetRepository, jdbcTemplate, testUsers);
		testRuleSets.rememberActive();
	}

	@AfterEach
	void tearDown() {
		testRuleSets.restoreActive();
	}

	// --- accesso ---

	@Test
	@DisplayName("Without a token the admin list returns 401")
	void findAll_withoutToken_returns401() throws Exception {
		// Act + Assert
		mockMvc.perform(get(BASE_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("A USER cannot read the admin list")
	void findAll_userRole_returns403() throws Exception {
		// Act + Assert
		mockMvc.perform(asUser(get(BASE_PATH)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	@DisplayName("A USER cannot create a rule set, and nothing is saved")
	void create_userRole_returns403AndSavesNothing() throws Exception {
		// Arrange
		RuleSetRequest request = request(uniqueVersion());

		// Act + Assert
		mockMvc.perform(asUser(post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content(toJson(request)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		assertThat(ruleSetRepository.existsByVersion(request.version())).isFalse();
	}

	@Test
	@DisplayName("A USER cannot update a rule set")
	void update_userRole_returns403() throws Exception {
		// Arrange
		RuleSetResponse created = testRuleSets.create();

		// Act + Assert
		mockMvc.perform(asUser(put(BASE_PATH + "/" + created.id())).contentType(MediaType.APPLICATION_JSON).content(toJson(request(uniqueVersion()))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		assertThat(ruleSetRepository.findById(created.id()).orElseThrow().getVersion()).isEqualTo(created.version());
	}

	@Test
	@DisplayName("A USER cannot activate a rule set")
	void activate_userRole_returns403() throws Exception {
		// Arrange
		RuleSetResponse created = testRuleSets.create();

		// Act + Assert
		mockMvc.perform(asUser(post(BASE_PATH + "/" + created.id() + "/activate")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		assertThat(ruleSetRepository.findById(created.id()).orElseThrow().isActive()).isFalse();
	}

	// --- GET ---

	@Test
	@DisplayName("An ADMIN gets every version, each with its active flag")
	void findAll_adminRole_returnsVersions() throws Exception {
		// Arrange
		RuleSetResponse created = testRuleSets.create();

		// Act + Assert
		mockMvc.perform(asAdmin(get(BASE_PATH)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", hasItem(created.id())))
			.andExpect(jsonPath("$[?(@.id == " + created.id() + ")].version").value(created.version()))
			.andExpect(jsonPath("$[?(@.id == " + created.id() + ")].isActive").value(false));
	}

	// --- POST ---

	@Test
	@DisplayName("An ADMIN creates a rule set: 201 with the saved, inactive rule set")
	void create_adminRole_returns201() throws Exception {
		// Arrange
		RuleSetRequest request = request(uniqueVersion());

		// Act + Assert
		postRuleSet(toJson(request))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.version").value(request.version()))
			.andExpect(jsonPath("$.isActive").value(false))
			.andExpect(jsonPath("$.startingLifePoints").value(30))
			.andExpect(jsonPath("$.createdAt").isNotEmpty());
		assertThat(ruleSetRepository.existsByVersion(request.version())).isTrue();
	}

	@Test
	@DisplayName("Missing fields return 400 with every missing field")
	void create_missingFields_returns400() throws Exception {
		// Act + Assert
		postRuleSet("{}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItems("version", "startingLifePoints", "maxRounds", "deckSize",
				"deckCreatures", "deckSpells", "deckHidden", "deckItems", "deckTerrains", "maxHiddenSurvival",
				"maxHiddenDamage", "signupBonus", "winReward", "drawReward")));
	}

	@Test
	@DisplayName("Values below the minimums return 400 with every wrong field")
	void create_valuesBelowMinimum_returns400() throws Exception {
		// Arrange
		RuleSetRequest request = new RuleSetRequest(" ", 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1);

		// Act + Assert
		postRuleSet(toJson(request))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItems("version", "startingLifePoints", "maxRounds", "deckSize",
				"deckCreatures", "deckSpells", "deckHidden", "deckItems", "deckTerrains", "maxHiddenSurvival",
				"maxHiddenDamage", "signupBonus", "winReward", "drawReward")));
	}

	@Test
	@DisplayName("Deck parts that do not add up to the deck size return 422")
	void create_deckPartsMismatch_returns422() throws Exception {
		// Act + Assert
		postRuleSet(toJson(requestWithDeck(uniqueVersion(), 30, 20, 6, 2, 1, 2)))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("RULE_SET_RULES_VIOLATED"));
	}

	@Test
	@DisplayName("A version already in use returns 409")
	void create_versionAlreadyUsed_returns409() throws Exception {
		// Arrange
		RuleSetResponse existing = testRuleSets.create();

		// Act + Assert
		postRuleSet(toJson(request(existing.version())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("RULE_SET_VERSION_TAKEN"));
	}

	// --- PUT ---

	@Test
	@DisplayName("An ADMIN updates a rule set never used: 200 with the new values, saved in the database")
	void update_adminRole_returnsUpdatedRuleSet() throws Exception {
		// Arrange
		RuleSetResponse created = testRuleSets.create();
		RuleSetRequest request = new RuleSetRequest(uniqueVersion(), 25, 60, 32, 20, 7, 2, 2, 1, 2, 1, 400, 40, 15);

		// Act + Assert
		putRuleSet(created.id(), toJson(request))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(request.version()))
			.andExpect(jsonPath("$.startingLifePoints").value(25))
			.andExpect(jsonPath("$.deckSize").value(32));
		assertThat(ruleSetRepository.findById(created.id()).orElseThrow().getVersion()).isEqualTo(request.version());
	}

	@Test
	@DisplayName("Updating a rule set that does not exist returns 404")
	void update_unknownId_returns404() throws Exception {
		// Act + Assert
		putRuleSet(Integer.MAX_VALUE, toJson(request(uniqueVersion())))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("RULE_SET_NOT_FOUND"));
	}

	@Test
	@DisplayName("Updating a rule set already used by a battle returns 409")
	void update_usedByBattle_returns409() throws Exception {
		// Arrange
		RuleSetResponse created = testRuleSets.create();
		testRuleSets.markUsedByBattle(created.id());

		// Act + Assert
		putRuleSet(created.id(), toJson(request(uniqueVersion())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("RULE_SET_IN_USE"));
	}

	@Test
	@DisplayName("Updating with invalid values returns 400")
	void update_invalidValues_returns400() throws Exception {
		// Arrange
		RuleSetResponse created = testRuleSets.create();

		// Act + Assert
		putRuleSet(created.id(), "{}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	// --- activate ---

	@Test
	@DisplayName("An ADMIN activates a rule set: 200, and the previous one is deactivated")
	void activate_adminRole_deactivatesThePreviousOne() throws Exception {
		// Arrange
		RuleSetResponse first = testRuleSets.create();
		RuleSetResponse second = testRuleSets.create();
		ruleSetService.activate(first.id());

		// Act + Assert
		mockMvc.perform(asAdmin(post(BASE_PATH + "/" + second.id() + "/activate")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(second.id()))
			.andExpect(jsonPath("$.isActive").value(true));
		assertThat(ruleSetRepository.findByIsActiveTrue()).map(RuleSet::getId).contains(second.id());
	}

	@Test
	@DisplayName("Activating a rule set that does not exist returns 404")
	void activate_unknownId_returns404() throws Exception {
		// Act + Assert
		mockMvc.perform(asAdmin(post(BASE_PATH + "/" + Integer.MAX_VALUE + "/activate")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("RULE_SET_NOT_FOUND"));
	}

	// --- documentazione OpenAPI ---

	@Test
	@DisplayName("OpenAPI requires the Bearer token on every admin rule set endpoint")
	void apiDocs_adminRuleSets_requireBearerToken() throws Exception {
		// Act + Assert: @SecurityRequirement è sulla classe, quindi vale per tutti i metodi
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/admin/rule-sets'].get.security[0]").value(hasKey("bearerAuth")))
			.andExpect(jsonPath("$.paths['/api/v1/admin/rule-sets'].post.security[0]").value(hasKey("bearerAuth")))
			.andExpect(jsonPath("$.paths['/api/v1/admin/rule-sets/{id}'].put.security[0]").value(hasKey("bearerAuth")))
			.andExpect(jsonPath("$.paths['/api/v1/admin/rule-sets/{id}/activate'].post.security[0]").value(hasKey("bearerAuth")));
	}

	// --- supporto ---

	private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.createAdmin()));
	}

	private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.create()));
	}

	private ResultActions postRuleSet(String json) throws Exception {
		return mockMvc.perform(asAdmin(post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions putRuleSet(Integer id, String json) throws Exception {
		return mockMvc.perform(asAdmin(put(BASE_PATH + "/" + id)).contentType(MediaType.APPLICATION_JSON).content(json));
	}
}
