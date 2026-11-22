package it.downfall.reversky.api.collection.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.collection.service.CollectionService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestCollections;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * {@link CollectionController} via HTTP: tutti gli endpoint chiedono un token.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class CollectionControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private CollectionService collectionService;

	@Autowired
	private CatalogCardRepository catalogCardRepository;

	@Autowired
	private RarityRepository rarityRepository;

	@Autowired
	private FactionRepository factionRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TestUsers testUsers;

	// Non serve al test: stessa configurazione degli altri ControllerIT, così Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	private TestCards testCards;

	private TestCollections testCollections;

	@BeforeEach
	void setUp() {
		testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		testCollections = new TestCollections(collectionService, transactionTemplate, jdbcTemplate);
	}

	@Test
	@DisplayName("Without a token every collection endpoint returns 401")
	void collectionEndpoints_withoutToken_return401() throws Exception {
		mockMvc.perform(get("/api/v1/collection")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/collection/summary")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/users/1/collection")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("GET /collection returns my cards with the card object, quantity, reserved copies and decks")
	void findMine_returnsEntries() throws Exception {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		User user = testUsers.create();
		CatalogCard card = testCards.create("Card " + token, CardType.CREATURE);
		testCollections.give(user, card, 2);
		testCollections.createDeck(user, Map.of(card, 1));

		// Act + Assert
		mockMvc.perform(as(user, get("/api/v1/collection")).param("name", token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].card.id").value(card.getId()))
			.andExpect(jsonPath("$.content[0].card.faction.name").value(testCards.faction().getName()))
			.andExpect(jsonPath("$.content[0].card.rarity.name").value(testCards.common().getName()))
			.andExpect(jsonPath("$.content[0].quantity").value(2))
			.andExpect(jsonPath("$.content[0].reservedQuantity").value(0))
			.andExpect(jsonPath("$.content[0].usedInDecks").value(1));
	}

	@Test
	@DisplayName("GET /collection with sort=quantity,desc sorts; an unknown sort property is 400 INVALID_COLLECTION_SORT")
	void findMine_sortParameter() throws Exception {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		User user = testUsers.create();
		CatalogCard few = testCards.create("A " + token, CardType.CREATURE);
		CatalogCard many = testCards.create("B " + token, CardType.CREATURE);
		testCollections.give(user, few, 1);
		testCollections.give(user, many, 3);

		// Act + Assert
		mockMvc.perform(as(user, get("/api/v1/collection")).param("name", token).param("sort", "quantity,desc"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].card.id").value(many.getId()))
			.andExpect(jsonPath("$.content[1].card.id").value(few.getId()));
		mockMvc.perform(as(user, get("/api/v1/collection")).param("sort", "atk"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_COLLECTION_SORT"));
	}

	@Test
	@DisplayName("GET /collection/summary returns owned and total cards, overall and per rarity")
	void summary_returnsProgress() throws Exception {
		// Arrange
		User user = testUsers.create();
		CatalogCard card = testCards.create(TestCards.uniqueName("Card"), CardType.CREATURE, testCards.createRarity());
		testCollections.give(user, card, 1);

		// Act + Assert
		mockMvc.perform(as(user, get("/api/v1/collection/summary")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.owned").value(1))
			.andExpect(jsonPath("$.total").isNumber())
			.andExpect(jsonPath("$.rarities[?(@.rarity.id == " + card.getRarity().getId() + ")].owned").value(1))
			.andExpect(jsonPath("$.rarities[?(@.rarity.id == " + card.getRarity().getId() + ")].total").value(1));
	}

	@Test
	@DisplayName("GET /users/{id}/collection shows another user's cards with available copies; unknown user is 404")
	void findOf_otherUser() throws Exception {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		User viewer = testUsers.create();
		User owner = testUsers.create();
		CatalogCard card = testCards.create("Card " + token, CardType.CREATURE);
		testCollections.give(owner, card, 2);

		// Act + Assert
		mockMvc.perform(as(viewer, get("/api/v1/users/" + owner.getId() + "/collection")).param("name", token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].card.id").value(card.getId()))
			.andExpect(jsonPath("$.content[0].availableQuantity").value(2))
			.andExpect(jsonPath("$.content[0].quantity").doesNotExist())
			.andExpect(jsonPath("$.content[0].usedInDecks").doesNotExist());
		mockMvc.perform(as(viewer, get("/api/v1/users/" + Integer.MAX_VALUE + "/collection")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	@DisplayName("OpenAPI documents the collection endpoints with their filters and the sort parameter")
	void apiDocs_documentCollection() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/collection'].get.parameters[*].name",
				hasItems("type", "name", "rarityId", "factionId", "page", "size", "sort")))
			.andExpect(jsonPath("$.paths['/api/v1/collection/summary'].get").exists())
			.andExpect(jsonPath("$.paths['/api/v1/users/{id}/collection'].get").exists())
			.andExpect(jsonPath("$.components.schemas.CollectionEntryResponse.properties.reservedQuantity").exists())
			.andExpect(jsonPath("$.components.schemas.AvailableCardResponse.properties.availableQuantity").exists());
	}

	private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}
}
