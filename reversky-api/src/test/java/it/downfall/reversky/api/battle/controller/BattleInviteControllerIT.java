package it.downfall.reversky.api.battle.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import it.downfall.reversky.api.deck.service.DeckService;
import it.downfall.reversky.api.friendship.service.FriendshipService;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.support.TestBattles;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestCollections;
import it.downfall.reversky.api.support.TestDecks;
import it.downfall.reversky.api.support.TestFriends;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Inviti alle sfide amichevoli via HTTP (API-37, API-38, API-39): amicizia, mazzo valido, liste, rifiuto e ritiro.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class BattleInviteControllerIT {

	private static final String BASE_PATH = "/api/v1/battle-invites";

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

	@Autowired
	private DeckService deckService;

	@Autowired
	private FriendshipService friendshipService;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	private TestBattles testBattles;

	@BeforeEach
	void setUp() {
		TestCards testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		TestDecks testDecks = new TestDecks(testCards, new TestCollections(collectionService, transactionTemplate, jdbcTemplate), ruleSetService);
		testBattles = new TestBattles(testDecks, deckService, new TestFriends(friendshipService, jdbcTemplate));
	}

	@Test
	@DisplayName("A friend is challenged with a valid deck; both boxes list it, only the challenger sees the deck")
	void create_friendWithValidDeck_listsInBothBoxes() throws Exception {
		// Arrange
		User challenger = testUsers.create();
		User opponent = testUsers.create();
		testBattles.makeFriends(challenger, opponent);
		Integer deckId = testBattles.playableDeck(challenger);

		// Act
		String body = invite(challenger, opponent.getId(), deckId)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.challenger.username").value(challenger.getUsername()))
			.andExpect(jsonPath("$.opponent.id").value(opponent.getId()))
			.andExpect(jsonPath("$.deck.id").value(deckId))
			.andReturn().getResponse().getContentAsString();
		String inviteId = JsonPath.read(body, "$.id");

		// Assert
		box(challenger, "outgoing")
			.andExpect(jsonPath("$[0].id").value(inviteId))
			.andExpect(jsonPath("$[0].deck.name").isNotEmpty());
		box(opponent, "incoming")
			.andExpect(jsonPath("$[0].id").value(inviteId))
			.andExpect(jsonPath("$[0].challenger.id").value(challenger.getId()))
			.andExpect(jsonPath("$[0].deck").isEmpty());
	}

	@Test
	@DisplayName("Challenging someone who is not a friend returns 403 and creates nothing")
	void create_notFriend_returns403() throws Exception {
		// Arrange
		User challenger = testUsers.create();
		User stranger = testUsers.create();
		Integer deckId = testBattles.playableDeck(challenger);

		// Act + Assert
		invite(challenger, stranger.getId(), deckId)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("NOT_FRIENDS"));
		box(challenger, "outgoing").andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Challenging myself returns 422, an unknown opponent 404, someone else's deck 404")
	void create_invalidTargets() throws Exception {
		// Arrange
		User challenger = testUsers.create();
		User friend = testUsers.create();
		User other = testUsers.create();
		testBattles.makeFriends(challenger, friend);
		Integer deckId = testBattles.playableDeck(challenger);
		Integer otherDeck = testBattles.playableDeck(other);

		// Act + Assert
		invite(challenger, challenger.getId(), deckId)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("BATTLE_SELF_CHALLENGE"));
		invite(challenger, Integer.MAX_VALUE, deckId).andExpect(status().isNotFound());
		invite(challenger, friend.getId(), otherDeck)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("DECK_NOT_FOUND"));
	}

	@Test
	@DisplayName("A deck that is no longer valid returns 422 DECK_INVALID")
	void create_invalidDeck_returns422() throws Exception {
		// Arrange: una carta del mazzo esce dalla collezione dopo il salvataggio
		User challenger = testUsers.create();
		User friend = testUsers.create();
		testBattles.makeFriends(challenger, friend);
		Integer deckId = testBattles.playableDeck(challenger);
		jdbcTemplate.update("DELETE FROM collection_cards WHERE user_id = ? AND card_id = "
			+ "(SELECT card_id FROM deck_cards WHERE deck_id = ? LIMIT 1)", challenger.getId(), deckId);

		// Act + Assert
		invite(challenger, friend.getId(), deckId)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("DECK_INVALID"))
			.andExpect(jsonPath("$.violations[*].code").value(hasItem("DECK_NOT_OWNED")));
	}

	@Test
	@DisplayName("A second pending challenge to the same friend returns 409; one in the other direction is fine")
	void create_secondPending_returns409() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testBattles.makeFriends(a, b);
		Integer deckA = testBattles.playableDeck(a);
		Integer deckB = testBattles.playableDeck(b);
		invite(a, b.getId(), deckA).andExpect(status().isCreated());

		// Act + Assert
		invite(a, b.getId(), deckA)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVITE_ALREADY_PENDING"));
		invite(b, a.getId(), deckB).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("The opponent rejects and the challenger withdraws: the rows are deleted; others get 404")
	void rejectAndWithdraw_deleteRows() throws Exception {
		// Arrange
		User challenger = testUsers.create();
		User opponent = testUsers.create();
		testBattles.makeFriends(challenger, opponent);
		Integer deckId = testBattles.playableDeck(challenger);
		String first = createdId(challenger, opponent, deckId);

		// Act + Assert: lo sfidante non può rifiutare, l'avversario sì
		mockMvc.perform(as(challenger, post(BASE_PATH + "/" + first + "/reject"))).andExpect(status().isNotFound());
		mockMvc.perform(as(opponent, post(BASE_PATH + "/" + first + "/reject"))).andExpect(status().isNoContent());
		assertThat(count(first)).isZero();

		String second = createdId(challenger, opponent, deckId);
		mockMvc.perform(as(opponent, delete(BASE_PATH + "/" + second))).andExpect(status().isNotFound());
		mockMvc.perform(as(challenger, delete(BASE_PATH + "/" + second))).andExpect(status().isNoContent());
		assertThat(count(second)).isZero();
	}

	@Test
	@DisplayName("An unknown box returns 400")
	void findBox_unknownBox_returns400() throws Exception {
		box(testUsers.create(), "sideways")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_BOX"));
	}

	private String createdId(User challenger, User opponent, Integer deckId) throws Exception {
		String body = invite(challenger, opponent.getId(), deckId).andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private ResultActions invite(User challenger, Integer opponentId, Integer deckId) throws Exception {
		return mockMvc.perform(as(challenger, post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON)
			.content("{\"opponentId\": %d, \"deckId\": %d}".formatted(opponentId, deckId)));
	}

	private ResultActions box(User user, String box) throws Exception {
		return mockMvc.perform(as(user, get(BASE_PATH)).param("box", box));
	}

	private Integer count(String inviteId) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM battle_invites WHERE id = ?::uuid", Integer.class, inviteId);
	}

	private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}
}
