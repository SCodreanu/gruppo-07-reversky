package it.downfall.reversky.api.deck.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * {@link DeckController} via HTTP, con i filtri di Spring Security attivi: nome, proprietà ed eliminazione dei mazzi
 * (API-26). Contenuto e validazione hanno i loro test.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class DeckControllerIT {

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

	private TestDecks testDecks;

	@BeforeEach
	void setUp() {
		TestCards testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		testDecks = new TestDecks(testCards, new TestCollections(collectionService, transactionTemplate, jdbcTemplate), ruleSetService);
	}

	@Test
	@DisplayName("Without a token the decks return 401")
	void findMine_withoutToken_returns401() throws Exception {
		// Act + Assert
		mockMvc.perform(get(BASE_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("A new deck is created with the trimmed name and shows up in my list, sorted by name")
	void create_validName_returns201AndListsSortedByName() throws Exception {
		// Arrange
		User user = testUsers.create();

		// Act
		create(user, "  Zeta  ")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.name").value("Zeta"));
		create(user, "Alpha").andExpect(status().isCreated());

		// Assert
		mockMvc.perform(as(user, get(BASE_PATH)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name").value(contains("Alpha", "Zeta")));
	}

	@Test
	@DisplayName("Two decks of the same user cannot share a name; another user can use it")
	void create_duplicateName_returns409OnlyForSameUser() throws Exception {
		// Arrange
		User user = testUsers.create();
		User other = testUsers.create();
		create(user, "Rain").andExpect(status().isCreated());

		// Act + Assert
		create(user, "Rain")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DECK_NAME_ALREADY_USED"));
		create(other, "Rain").andExpect(status().isCreated());
	}

	@Test
	@DisplayName("A blank name or a name longer than 50 characters returns 400")
	void create_invalidName_returns400() throws Exception {
		// Arrange
		User user = testUsers.create();

		// Act + Assert
		create(user, "   ")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(hasItem("name")));
		create(user, "x".repeat(51))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(hasItem("name")));
		create(user, "x".repeat(50)).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("The deck of another user is not found, for read, update and delete")
	void otherUsersDeck_returns404() throws Exception {
		// Arrange
		User owner = testUsers.create();
		User stranger = testUsers.create();
		Integer deckId = createdId(owner, "Mine");

		// Act + Assert
		mockMvc.perform(as(stranger, get(BASE_PATH + "/" + deckId)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("DECK_NOT_FOUND"));
		update(stranger, deckId, "Stolen").andExpect(status().isNotFound());
		mockMvc.perform(as(stranger, delete(BASE_PATH + "/" + deckId))).andExpect(status().isNotFound());
		mockMvc.perform(as(owner, get(BASE_PATH + "/" + deckId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Mine"));
	}

	@Test
	@DisplayName("Renaming keeps the same name or takes a free one, but not the name of another of my decks")
	void update_rename_checksUniqueness() throws Exception {
		// Arrange
		User user = testUsers.create();
		Integer deckId = createdId(user, "First");
		createdId(user, "Second");

		// Act + Assert
		update(user, deckId, "First").andExpect(status().isOk());
		update(user, deckId, "Second")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DECK_NAME_ALREADY_USED"));
		update(user, deckId, "Third")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Third"));
	}

	@Test
	@DisplayName("Updating a deck in the ranked queue removes the ticket")
	void update_deckInQueue_removesTicket() throws Exception {
		// Arrange
		User user = testUsers.create();
		Integer deckId = createdId(user, "Queued");
		jdbcTemplate.update("INSERT INTO matchmaking_queue (user_id, deck_id, rating) VALUES (?, ?, 1000)", user.getId(), deckId);

		// Act
		update(user, deckId, "Queued again").andExpect(status().isOk());

		// Assert
		assertThat(queueTickets(user)).isZero();
	}

	@Test
	@DisplayName("Deleting a deck returns 204, removes it and its queue ticket")
	void delete_ownDeck_returns204() throws Exception {
		// Arrange
		User user = testUsers.create();
		Integer deckId = createdId(user, "Old");
		jdbcTemplate.update("INSERT INTO matchmaking_queue (user_id, deck_id, rating) VALUES (?, ?, 1000)", user.getId(), deckId);

		// Act
		mockMvc.perform(as(user, delete(BASE_PATH + "/" + deckId))).andExpect(status().isNoContent());

		// Assert
		mockMvc.perform(as(user, get(BASE_PATH + "/" + deckId))).andExpect(status().isNotFound());
		assertThat(queueTickets(user)).isZero();
	}

	@Test
	@DisplayName("A deck used by a pending challenge cannot be deleted; an accepted one can")
	void delete_deckInPendingInvite_returns409() throws Exception {
		// Arrange: invito scritto con SQL, così il test non dipende dal flusso degli inviti (il controllo passa da
		// PendingInviteService)
		User challenger = testUsers.create();
		User opponent = testUsers.create();
		Integer deckId = createdId(challenger, "Challenge");
		UUID inviteId = UUID.randomUUID();
		jdbcTemplate.update("INSERT INTO battle_invites (id, challenger_id, challenger_deck_id, opponent_id) VALUES (?, ?, ?, ?)",
			inviteId, challenger.getId(), deckId, opponent.getId());

		// Act + Assert
		mockMvc.perform(as(challenger, delete(BASE_PATH + "/" + deckId)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DECK_IN_USE"));

		jdbcTemplate.update("UPDATE battle_invites SET status = 'ACCEPTED' WHERE id = ?", inviteId);
		mockMvc.perform(as(challenger, delete(BASE_PATH + "/" + deckId))).andExpect(status().isNoContent());
		assertThat(jdbcTemplate.queryForObject("SELECT challenger_deck_id FROM battle_invites WHERE id = ?", Integer.class, inviteId))
			.isNull();
	}

	@Test
	@DisplayName("OpenAPI requires the Bearer token on /decks")
	void apiDocs_decks_requiresBearerToken() throws Exception {
		// Act + Assert
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/decks'].get.security[0]").value(hasKey("bearerAuth")));
	}

	// Ogni richiesta porta un contenuto valido nuovo, con le carte già nella collezione dell'utente
	private ResultActions create(User user, String name) throws Exception {
		return mockMvc.perform(as(user, post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content(body(user, name)));
	}

	private ResultActions update(User user, Integer deckId, String name) throws Exception {
		return mockMvc.perform(as(user, put(BASE_PATH + "/" + deckId)).contentType(MediaType.APPLICATION_JSON).content(body(user, name)));
	}

	private Integer createdId(User user, String name) throws Exception {
		String response = create(user, name).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private String body(User user, String name) {
		return TestDecks.requestJson(name, testDecks.validContent(user));
	}

	private Integer queueTickets(User user) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM matchmaking_queue WHERE user_id = ?", Integer.class, user.getId());
	}

	private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}
}
