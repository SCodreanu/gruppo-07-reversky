package it.downfall.reversky.api.ruleset.controller;

import static it.downfall.reversky.api.support.TestUsers.uniqueSuffix;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.dto.FactionRequest;
import it.downfall.reversky.api.catalog.dto.FactionResponse;
import it.downfall.reversky.api.catalog.service.FactionService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.factionbonus.dto.FactionBonusRequest;
import it.downfall.reversky.api.factionbonus.service.FactionBonusService;
import it.downfall.reversky.api.ruleset.dto.RuleSetResponse;
import it.downfall.reversky.api.ruleset.repository.RuleSetRepository;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.support.TestRuleSets;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link RuleSetController} via HTTP: GET /rules è pubblico.
 * <p>
 * Il rule set attivo prima del test si rimette attivo dopo (vedi {@link TestRuleSets}).
 * I valori attesi si derivano dal rule set creato: {@link TestRuleSets} può cambiarli senza
 * rompere questi test.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class RuleSetControllerIT {

	private static final String BASE_PATH = "/api/v1/rules";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RuleSetService ruleSetService;

	@Autowired
	private RuleSetRepository ruleSetRepository;

	@Autowired
	private FactionService factionService;

	@Autowired
	private FactionBonusService factionBonusService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TestUsers testUsers;

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

	@Test
	@DisplayName("Without a token GET /rules returns the active rule set, without admin fields")
	void findActive_withoutToken_returnsActiveRuleSet() throws Exception {
		// Arrange
		RuleSetResponse created = testRuleSets.create();
		ruleSetService.activate(created.id());

		// Act + Assert: i valori si leggono dalla risposta del create, non hardcoded
		mockMvc.perform(get(BASE_PATH))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(created.version()))
			.andExpect(jsonPath("$.startingLifePoints").value(created.startingLifePoints()))
			.andExpect(jsonPath("$.maxRounds").value(created.maxRounds()))
			.andExpect(jsonPath("$.deckCreatures").value(created.deckCreatures()))
			.andExpect(jsonPath("$.signupBonus").value(created.signupBonus()))
			.andExpect(jsonPath("$.id").doesNotExist())
			.andExpect(jsonPath("$.isActive").doesNotExist())
			.andExpect(jsonPath("$.createdById").doesNotExist());
	}

	@Test
	@DisplayName("Without an active rule set GET /rules returns 404")
	void findActive_noneActive_returns404() throws Exception {
		// Arrange
		jdbcTemplate.update("UPDATE rule_sets SET is_active = false WHERE is_active");

		// Act + Assert
		mockMvc.perform(get(BASE_PATH))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NO_ACTIVE_RULE_SET"));
	}

	@Test
	@DisplayName("GET /rules includes the current faction bonuses")
	void findActive_includesFactionBonuses() throws Exception {
		// Arrange: due fazioni e una coppia di bonus
		FactionResponse a = factionService.create(new FactionRequest("Faction A " + uniqueSuffix(), "factions/a.svg", null));
		FactionResponse b = factionService.create(new FactionRequest("Faction B " + uniqueSuffix(), "factions/b.svg", null));

		try {
			factionBonusService.replaceAll(List.of(
				new FactionBonusRequest(a.id(), b.id(), 2, 1),
				new FactionBonusRequest(b.id(), a.id(), -1, 0)));

			RuleSetResponse created = testRuleSets.create();
			ruleSetService.activate(created.id());

			mockMvc.perform(get(BASE_PATH))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.factionBonuses", hasSize(2)))
				.andExpect(jsonPath("$.factionBonuses[?(@.originFactionId == " + a.id()
					+ " && @.targetFactionId == " + b.id() + ")].atkBonus").value(2))
				.andExpect(jsonPath("$.factionBonuses[?(@.originFactionId == " + b.id()
					+ " && @.targetFactionId == " + a.id() + ")].atkBonus").value(-1));
		} finally {
			// La matrice dei bonus è globale: non lasciarla sporca agli altri IT
			factionBonusService.replaceAll(List.of());
		}
	}

	@Test
	@DisplayName("GET /rules with an empty bonus matrix returns an empty list, not a missing field")
	void findActive_emptyBonusMatrix_returnsEmptyList() throws Exception {
		// Arrange
		factionBonusService.replaceAll(List.of());
		RuleSetResponse created = testRuleSets.create();
		ruleSetService.activate(created.id());

		// Act + Assert
		mockMvc.perform(get(BASE_PATH))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.factionBonuses").isArray())
			.andExpect(jsonPath("$.factionBonuses", hasSize(0)));
	}

	@Test
	@DisplayName("OpenAPI does not ask for a token on GET /rules")
	void apiDocs_rules_hasNoSecurity() throws Exception {
		// Act + Assert
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/rules'].get").exists())
			.andExpect(jsonPath("$.paths['/api/v1/rules'].get.security").doesNotExist());
	}
}