package it.downfall.reversky.api.deck.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

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
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.collection.service.CollectionService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.ruleset.entity.RuleSet;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestCollections;
import it.downfall.reversky.api.support.TestDecks;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Validazione dei mazzi via HTTP (API-28): POST /decks/validate, GET /decks/rules, salvataggio rifiutato e
 * {@code valid} ricalcolato a ogni lettura. Ogni regola nel dettaglio è in DeckValidatorTest.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class DeckValidationControllerIT {

	private static final String BASE_PATH = "/api/v1/decks";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CatalogCardRepository catalogCardRepository;

	@Autowired
	private RarityRepository rarityRepository;

	@Autowired
	private FactionRepository factionRepository;

	@Autowired
	private CollectionService collectionService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private RuleSetService ruleSetService;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	private TestCards testCards;
	private TestDecks testDecks;

	@BeforeEach
	void setUp() {
		testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		testDecks = new TestDecks(testCards, new TestCollections(collectionService, transactionTemplate, jdbcTemplate), ruleSetService);
	}

	@Test
	@DisplayName("A valid content validates with no violations and nothing is saved")
	void validate_validContent_returnsValid() throws Exception {
		// Arrange
		User user = testUsers.create();
		String json = "{\"cards\": " + TestDecks.toJson(testDecks.validContent(user)) + "}";

		// Act + Assert
		validate(user, json)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(true))
			.andExpect(jsonPath("$.violations.length()").value(0));
		mockMvc.perform(as(user, get(BASE_PATH))).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("An invalid content validates with 200 and every violation, unknown cards included")
	void validate_invalidContent_returnsAllViolations() throws Exception {
		// Arrange: senza il Terreno e con una carta che non esiste
		User user = testUsers.create();
		Map<CatalogCard, Integer> content = withoutTerrain(testDecks.validContent(user));
		String cards = TestDecks.toJson(content).replace("]", ", {\"cardId\": " + Integer.MAX_VALUE + ", \"quantity\": 1}]");

		// Act + Assert
		validate(user, "{\"cards\": " + cards + "}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(false))
			.andExpect(jsonPath("$.violations[0].code").value("DECK_CARD_NOT_FOUND"))
			.andExpect(jsonPath("$.violations[*].code").value(hasItem("DECK_SIZE")))
			.andExpect(jsonPath("$.violations[*].code").value(hasItem("DECK_TERRAIN_COUNT")));
	}

	@Test
	@DisplayName("The same card twice in the validation request returns 400")
	void validate_duplicateCard_returns400() throws Exception {
		// Arrange
		User user = testUsers.create();
		CatalogCard card = testCards.createCreature();
		String json = "{\"cards\": [{\"cardId\": %d, \"quantity\": 1}, {\"cardId\": %d, \"quantity\": 1}]}"
			.formatted(card.getId(), card.getId());

		// Act + Assert
		validate(user, json)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(hasItem("cardIdsUnique")));
	}

	@Test
	@DisplayName("Saving an invalid deck returns 422 DECK_INVALID with all the violations, and nothing is saved")
	void create_invalidDeck_returns422WithViolations() throws Exception {
		// Arrange
		User user = testUsers.create();
		Map<CatalogCard, Integer> content = withoutTerrain(testDecks.validContent(user));

		// Act + Assert
		mockMvc.perform(as(user, post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content(TestDecks.requestJson("Broken", content)))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("DECK_INVALID"))
			.andExpect(jsonPath("$.title").value("Invalid deck"))
			.andExpect(jsonPath("$.violations[0].code").value("DECK_SIZE"))
			.andExpect(jsonPath("$.violations[1].code").value("DECK_TERRAIN_COUNT"))
			.andExpect(jsonPath("$.violations[1].message").value("The deck needs exactly 1 Terrain, it has 0"));
		mockMvc.perform(as(user, get(BASE_PATH))).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("A saved deck becomes invalid when one of its cards is deactivated, and valid again when reactivated")
	void read_cardDeactivated_recomputesValid() throws Exception {
		// Arrange
		User user = testUsers.create();
		Map<CatalogCard, Integer> content = testDecks.validContent(user);
		Integer deckId = createdId(user, content);
		CatalogCard card = content.keySet().iterator().next();
		testCards.deactivate(card);

		// Act + Assert
		mockMvc.perform(as(user, get(BASE_PATH + "/" + deckId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(false))
			.andExpect(jsonPath("$.violations[0].code").value("DECK_INACTIVE_CARD"))
			.andExpect(jsonPath("$.violations[0].cardId").value(card.getId()));
		mockMvc.perform(as(user, get(BASE_PATH)))
			.andExpect(jsonPath("$[0].valid").value(false));

		jdbcTemplate.update("UPDATE catalog_cards SET is_active = true WHERE id = ?", card.getId());
		mockMvc.perform(as(user, get(BASE_PATH + "/" + deckId)))
			.andExpect(jsonPath("$.valid").value(true))
			.andExpect(jsonPath("$.violations.length()").value(0));
	}

	@Test
	@DisplayName("The deck rules come from the active rule set and the rarities")
	void rules_returnsActiveRuleSetAndRarities() throws Exception {
		// Arrange
		User user = testUsers.create();
		RuleSet active = ruleSetService.getActiveRuleSet();

		// Act + Assert
		mockMvc.perform(as(user, get(BASE_PATH + "/rules")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.deckSize").value(active.getDeckSize()))
			.andExpect(jsonPath("$.deckCreatures").value(active.getDeckCreatures()))
			.andExpect(jsonPath("$.deckSpells").value(active.getDeckSpells()))
			.andExpect(jsonPath("$.deckHidden").value(active.getDeckHidden()))
			.andExpect(jsonPath("$.deckItems").value(active.getDeckItems()))
			.andExpect(jsonPath("$.deckTerrains").value(active.getDeckTerrains()))
			.andExpect(jsonPath("$.maxHiddenSurvival").value(active.getMaxHiddenSurvival()))
			.andExpect(jsonPath("$.maxHiddenDamage").value(active.getMaxHiddenDamage()))
			.andExpect(jsonPath("$.rarities[0].sortingOrder").value(1))
			.andExpect(jsonPath("$.rarities[0].deckLimit").isNumber());
	}

	private Map<CatalogCard, Integer> withoutTerrain(Map<CatalogCard, Integer> content) {
		Map<CatalogCard, Integer> changed = new LinkedHashMap<>(content);
		changed.keySet().removeIf(card -> card.getCardType() == CardType.TERRAIN);
		return changed;
	}

	private Integer createdId(User user, Map<CatalogCard, Integer> content) throws Exception {
		String response = mockMvc.perform(as(user, post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON)
				.content(TestDecks.requestJson("Deck " + TestUsers.uniqueSuffix(), content)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private ResultActions validate(User user, String json) throws Exception {
		return mockMvc.perform(as(user, post(BASE_PATH + "/validate")).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}
}
