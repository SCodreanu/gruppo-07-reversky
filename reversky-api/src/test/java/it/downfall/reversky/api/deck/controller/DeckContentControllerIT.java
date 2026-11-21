package it.downfall.reversky.api.deck.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.jayway.jsonpath.JsonPath;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Contenuto dei mazzi via HTTP (API-27): carte e copie, sostituzione in modifica, carte inesistenti o ripetute.
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

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	private TestCards testCards;

	@BeforeEach
	void setUp() {
		testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
	}

	@Test
	@DisplayName("A deck is saved with its cards and quantities; detail sorts cards by id, list shows the count")
	void create_withCards_savesContent() throws Exception {
		// Arrange
		User user = testUsers.create();
		CatalogCard first = testCards.createCreature();
		CatalogCard second = testCards.createCreature();

		// Act
		Integer deckId = createdId(user, "Content", Map.of(second, 1, first, 2));

		// Assert
		mockMvc.perform(as(user, get(BASE_PATH + "/" + deckId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cardCount").value(3))
			.andExpect(jsonPath("$.cards[*].card.id").value(contains(first.getId(), second.getId())))
			.andExpect(jsonPath("$.cards[0].quantity").value(2))
			.andExpect(jsonPath("$.cards[0].card.name").value(first.getName()))
			.andExpect(jsonPath("$.cards[0].card.rarity.name").isNotEmpty());
		mockMvc.perform(as(user, get(BASE_PATH)))
			.andExpect(jsonPath("$[0].cardCount").value(3))
			.andExpect(jsonPath("$[0].cards").doesNotExist());
	}

	@Test
	@DisplayName("Updating replaces the content in place: kept cards change quantity, removed ones go, new ones arrive")
	void update_keepingSomeCards_replacesContent() throws Exception {
		// Arrange
		User user = testUsers.create();
		CatalogCard kept = testCards.createCreature();
		CatalogCard removed = testCards.createCreature();
		CatalogCard added = testCards.createCreature();
		Integer deckId = createdId(user, "Evolving", Map.of(kept, 1, removed, 2));

		// Act
		save(user, put(BASE_PATH + "/" + deckId), "Evolving", Map.of(kept, 3, added, 1))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cardCount").value(4));

		// Assert
		assertThat(rows(deckId)).containsExactlyInAnyOrderEntriesOf(Map.of(kept.getId(), 3, added.getId(), 1));
	}

	@Test
	@DisplayName("A card that does not exist returns 422 DECK_INVALID with DECK_CARD_NOT_FOUND, and nothing is saved")
	void create_unknownCard_returns422AndSavesNothing() throws Exception {
		// Arrange
		User user = testUsers.create();
		CatalogCard real = testCards.createCreature();
		String json = "{\"name\": \"Ghost\", \"cards\": [{\"cardId\": " + real.getId() + ", \"quantity\": 1}, "
			+ "{\"cardId\": " + Integer.MAX_VALUE + ", \"quantity\": 1}]}";

		// Act + Assert
		mockMvc.perform(as(user, post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content(json))
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
		CatalogCard card = testCards.createCreature();
		Integer deckId = createdId(user, "Stable", Map.of(card, 2));
		String json = "{\"name\": \"Renamed\", \"cards\": [{\"cardId\": " + Integer.MAX_VALUE + ", \"quantity\": 1}]}";

		// Act
		mockMvc.perform(as(user, put(BASE_PATH + "/" + deckId)).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isUnprocessableContent());

		// Assert
		mockMvc.perform(as(user, get(BASE_PATH + "/" + deckId)))
			.andExpect(jsonPath("$.name").value("Stable"))
			.andExpect(jsonPath("$.cardCount").value(2));
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
		Integer deckId = createdId(user, "Doomed", Map.of(testCards.createCreature(), 2));

		// Act
		mockMvc.perform(as(user, delete(BASE_PATH + "/" + deckId))).andExpect(status().isNoContent());

		// Assert
		assertThat(rows(deckId)).isEmpty();
	}

	private Integer createdId(User user, String name, Map<CatalogCard, Integer> cards) throws Exception {
		String response = save(user, post(BASE_PATH), name, cards)
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private ResultActions save(User user, MockHttpServletRequestBuilder request, String name, Map<CatalogCard, Integer> cards)
			throws Exception {
		String cardsJson = cards.entrySet().stream()
			.map(entry -> "{\"cardId\": %d, \"quantity\": %d}".formatted(entry.getKey().getId(), entry.getValue()))
			.collect(Collectors.joining(", "));
		String json = "{\"name\": \"" + name + "\", \"cards\": [" + cardsJson + "]}";
		return mockMvc.perform(as(user, request).contentType(MediaType.APPLICATION_JSON).content(json));
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
