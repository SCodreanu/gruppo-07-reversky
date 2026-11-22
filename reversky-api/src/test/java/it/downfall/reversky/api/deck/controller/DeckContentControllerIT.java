package it.downfall.reversky.api.deck.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestCollections;
import it.downfall.reversky.api.support.TestDecks;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Contenuto dei mazzi via HTTP (API-27): carte e copie, sostituzione in modifica, carte inesistenti o ripetute.
 * Le regole del mazzo sono in DeckValidatorTest e DeckValidationControllerIT.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class DeckContentControllerIT {

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
	@DisplayName("A deck is saved with its cards and quantities; detail sorts cards by id, list shows the count")
	void create_withCards_savesContent() throws Exception {
		// Arrange
		User user = testUsers.create();
		Map<CatalogCard, Integer> content = testDecks.validContent(user);
		CatalogCard lowest = content.keySet().stream().min(Comparator.comparing(CatalogCard::getId)).orElseThrow();
		int total = content.values().stream().mapToInt(Integer::intValue).sum();

		// Act
		Integer deckId = createdId(user, "Content", content);

		// Assert
		mockMvc.perform(as(user, get(BASE_PATH + "/" + deckId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cardCount").value(total))
			.andExpect(jsonPath("$.cards.length()").value(content.size()))
			.andExpect(jsonPath("$.cards[0].card.id").value(lowest.getId()))
			.andExpect(jsonPath("$.cards[0].quantity").value(content.get(lowest)))
			.andExpect(jsonPath("$.cards[0].card.name").value(lowest.getName()))
			.andExpect(jsonPath("$.cards[0].card.rarity.name").isNotEmpty());
		mockMvc.perform(as(user, get(BASE_PATH)))
			.andExpect(jsonPath("$[0].cardCount").value(total))
			.andExpect(jsonPath("$[0].cards").doesNotExist());
	}

	@Test
	@DisplayName("Updating replaces the content in place: kept cards change quantity, removed ones go, new ones arrive")
	void update_keepingSomeCards_replacesContent() throws Exception {
		// Arrange: con 20 creature TestDecks crea sei creature da 3 copie e una da 2. Una da 3 esce, una nuova da 3
		// entra, una da 3 scende a 2 e quella da 2 sale a 3: il totale resta lo stesso e il mazzo resta valido
		User user = testUsers.create();
		Map<CatalogCard, Integer> content = testDecks.validContent(user);
		Integer deckId = createdId(user, "Evolving", content);
		List<CatalogCard> fullCreatures = creaturesWith(content, 3);
		CatalogCard removed = fullCreatures.get(0);
		CatalogCard reduced = fullCreatures.get(1);
		CatalogCard raised = creaturesWith(content, 2).get(0);
		CatalogCard added = testCards.createCreature();
		testCollections.give(user, added, 3);
		testCollections.give(user, raised, 1);

		Map<CatalogCard, Integer> changed = new LinkedHashMap<>(content);
		changed.remove(removed);
		changed.put(reduced, 2);
		changed.put(raised, 3);
		changed.put(added, 3);

		// Act
		save(user, put(BASE_PATH + "/" + deckId), "Evolving", changed).andExpect(status().isOk());

		// Assert
		Map<Integer, Integer> expected = new LinkedHashMap<>();
		changed.forEach((card, quantity) -> expected.put(card.getId(), quantity));
		assertThat(rows(deckId)).containsExactlyInAnyOrderEntriesOf(expected);
	}

	@Test
	@DisplayName("A card that does not exist returns 422 DECK_INVALID with DECK_CARD_NOT_FOUND first, and nothing is saved")
	void create_unknownCard_returns422AndSavesNothing() throws Exception {
		// Arrange
		User user = testUsers.create();
		String cards = TestDecks.toJson(testDecks.validContent(user));
		String json = "{\"name\": \"Ghost\", \"cards\": " + cards.replace("]", ", {\"cardId\": " + Integer.MAX_VALUE + ", \"quantity\": 1}]") + "}";

		// Act + Assert
		postJson(user, json)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("DECK_INVALID"))
			.andExpect(jsonPath("$.violations[0].code").value("DECK_CARD_NOT_FOUND"))
			.andExpect(jsonPath("$.violations[0].cardId").value(Integer.MAX_VALUE));
		mockMvc.perform(as(user, get(BASE_PATH))).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("An update with a card that does not exist leaves the deck as it was")
	void update_unknownCard_leavesDeckUnchanged() throws Exception {
		// Arrange
		User user = testUsers.create();
		Map<CatalogCard, Integer> content = testDecks.validContent(user);
		Integer deckId = createdId(user, "Stable", content);
		String json = "{\"name\": \"Renamed\", \"cards\": [{\"cardId\": " + Integer.MAX_VALUE + ", \"quantity\": 1}]}";

		// Act
		mockMvc.perform(as(user, put(BASE_PATH + "/" + deckId)).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isUnprocessableContent());

		// Assert
		mockMvc.perform(as(user, get(BASE_PATH + "/" + deckId)))
			.andExpect(jsonPath("$.name").value("Stable"))
			.andExpect(jsonPath("$.cards.length()").value(content.size()));
	}

	@Test
	@DisplayName("The same card twice, a quantity below 1 or missing cards return 400")
	void create_invalidCards_returns400() throws Exception {
		// Arrange
		User user = testUsers.create();
		CatalogCard card = testCards.createCreature();
		String twice = "{\"name\": \"Twice\", \"cards\": [{\"cardId\": %d, \"quantity\": 1}, {\"cardId\": %d, \"quantity\": 2}]}"
			.formatted(card.getId(), card.getId());
		String zero = "{\"name\": \"Zero\", \"cards\": [{\"cardId\": %d, \"quantity\": 0}]}".formatted(card.getId());

		// Act + Assert
		postJson(user, twice)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(hasItem("cardIdsUnique")));
		postJson(user, zero)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(hasItem("cards[0].quantity")));
		postJson(user, "{\"name\": \"None\"}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(hasItem("cards")));
	}

	@Test
	@DisplayName("Deleting a deck deletes its deck_cards rows")
	void delete_deckWithCards_deletesRows() throws Exception {
		// Arrange
		User user = testUsers.create();
		Integer deckId = createdId(user, "Doomed", testDecks.validContent(user));

		// Act
		mockMvc.perform(as(user, delete(BASE_PATH + "/" + deckId))).andExpect(status().isNoContent());

		// Assert
		assertThat(rows(deckId)).isEmpty();
	}

	private static List<CatalogCard> creaturesWith(Map<CatalogCard, Integer> content, int copies) {
		return content.keySet().stream()
			.filter(card -> card.getCardType() == CardType.CREATURE && content.get(card) == copies)
			.toList();
	}

	private Integer createdId(User user, String name, Map<CatalogCard, Integer> cards) throws Exception {
		String response = save(user, post(BASE_PATH), name, cards)
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private ResultActions save(User user, MockHttpServletRequestBuilder request, String name, Map<CatalogCard, Integer> cards)
			throws Exception {
		return mockMvc.perform(as(user, request).contentType(MediaType.APPLICATION_JSON).content(TestDecks.requestJson(name, cards)));
	}

	private ResultActions postJson(User user, String json) throws Exception {
		return mockMvc.perform(as(user, post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private Map<Integer, Integer> rows(Integer deckId) {
		Map<Integer, Integer> rows = new LinkedHashMap<>();
		jdbcTemplate.query("SELECT card_id, quantity FROM deck_cards WHERE deck_id = ?",
			rs -> {
				rows.put(rs.getInt("card_id"), rs.getInt("quantity"));
			}, deckId);
		return rows;
	}

	private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}
}
