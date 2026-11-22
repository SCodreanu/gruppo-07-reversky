package it.downfall.reversky.api.deck.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

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
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestCollections;
import it.downfall.reversky.api.support.TestDecks;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Possesso delle carte del mazzo via HTTP (API-29): copie disponibili = possedute meno riservate dalle offerte di
 * scambio aperte, controllate al salvataggio, in POST /decks/validate e a ogni lettura.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class DeckOwnershipControllerIT {

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
	private TestCollections testCollections;
	private TestDecks testDecks;

	@BeforeEach
	void setUp() {
		testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		testCollections = new TestCollections(collectionService, transactionTemplate, jdbcTemplate);
		testDecks = new TestDecks(testCards, testCollections, ruleSetService);
	}

	@Test
	@DisplayName("A deck with cards of someone else's collection is rejected with DECK_NOT_OWNED")
	void create_cardsNotOwned_returns422() throws Exception {
		// Arrange: le carte sono nella collezione di un altro utente
		User owner = testUsers.create();
		User user = testUsers.create();
		Map<CatalogCard, Integer> content = testDecks.validContent(owner);

		// Act + Assert
		save(user, content)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("DECK_INVALID"))
			.andExpect(jsonPath("$.violations[*].code").value(hasItem("DECK_NOT_OWNED")));
	}

	@Test
	@DisplayName("Copies reserved by an open trade are not available; once the trade closes they are")
	void validate_copiesReservedByTrade_untilTradeCloses() throws Exception {
		// Arrange: una creatura da 3 copie nel mazzo, 2 offerte in uno scambio aperto -> 1 disponibile
		User user = testUsers.create();
		User other = testUsers.create();
		Map<CatalogCard, Integer> content = testDecks.validContent(user);
		CatalogCard creature = fullCreature(content);
		UUID tradeId = testCollections.createTradeOffer(user, other, "WAITING_RECIPIENT", user, creature, 2);
		String json = "{\"cards\": " + TestDecks.toJson(content) + "}";

		// Act + Assert
		validate(user, json)
			.andExpect(jsonPath("$.valid").value(false))
			.andExpect(jsonPath("$.violations[0].code").value("DECK_NOT_OWNED"))
			.andExpect(jsonPath("$.violations[0].cardId").value(creature.getId()))
			.andExpect(jsonPath("$.violations[0].message").value(
				"You have 1 available copy of '" + creature.getName() + "', the deck contains 3"));

		jdbcTemplate.update("UPDATE trades SET status = 'REJECTED' WHERE id = ?", tradeId);
		validate(user, json)
			.andExpect(jsonPath("$.valid").value(true));
	}

	@Test
	@DisplayName("An offer that is not the current proposal does not reserve copies")
	void validate_otherSidesTurn_doesNotReserve() throws Exception {
		// Arrange: con WAITING_REQUESTER la proposta in corso è del destinatario, quindi l'offerta del richiedente non
		// riserva niente (docs/scambi, "Copie riservate")
		User user = testUsers.create();
		User other = testUsers.create();
		Map<CatalogCard, Integer> content = testDecks.validContent(user);
		testCollections.createTradeOffer(user, other, "WAITING_REQUESTER", user, fullCreature(content), 2);

		// Act + Assert
		validate(user, "{\"cards\": " + TestDecks.toJson(content) + "}")
			.andExpect(jsonPath("$.valid").value(true));
	}

	@Test
	@DisplayName("After giving cards away a saved deck reads as invalid, but is not changed")
	void read_cardsGivenAway_invalidButUnchanged() throws Exception {
		// Arrange
		User user = testUsers.create();
		Map<CatalogCard, Integer> content = testDecks.validContent(user);
		Integer deckId = createdId(user, content);
		CatalogCard creature = fullCreature(content);
		jdbcTemplate.update("UPDATE collection_cards SET quantity = 1 WHERE user_id = ? AND card_id = ?", user.getId(), creature.getId());

		// Act + Assert
		mockMvc.perform(as(user, get(BASE_PATH + "/" + deckId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(false))
			.andExpect(jsonPath("$.violations[0].code").value("DECK_NOT_OWNED"))
			.andExpect(jsonPath("$.cards.length()").value(content.size()))
			.andExpect(jsonPath("$.cards[?(@.card.id == " + creature.getId() + ")].quantity").value(hasItem(3)));
		mockMvc.perform(as(user, get(BASE_PATH)))
			.andExpect(jsonPath("$[0].valid").value(false));
	}

	@Test
	@DisplayName("Owning exactly the deck quantities is enough")
	void create_exactlyOwnedCopies_succeeds() throws Exception {
		// Arrange
		User user = testUsers.create();

		// Act + Assert
		save(user, testDecks.validContent(user))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.valid").value(true))
			.andExpect(jsonPath("$.violations[*].code").value(not(hasItem("DECK_NOT_OWNED"))));
	}

	private static CatalogCard fullCreature(Map<CatalogCard, Integer> content) {
		return content.keySet().stream()
			.filter(card -> card.getCardType() == CardType.CREATURE && content.get(card) == 3)
			.findFirst().orElseThrow();
	}

	private Integer createdId(User user, Map<CatalogCard, Integer> content) throws Exception {
		String response = save(user, content).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private ResultActions save(User user, Map<CatalogCard, Integer> content) throws Exception {
		return mockMvc.perform(as(user, post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON)
			.content(TestDecks.requestJson("Deck " + TestUsers.uniqueSuffix(), content)));
	}

	private ResultActions validate(User user, String json) throws Exception {
		return mockMvc.perform(as(user, post(BASE_PATH + "/validate")).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isOk());
	}

	private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}
}
