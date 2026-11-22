package it.downfall.reversky.api.battle.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.battle.service.QueueJoinedEvent;
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
 * Coda classificata via HTTP (API-47, docs/sfide, "Classificata: matchmaking"): ingresso con un mazzo valido,
 * ticket, uscita e ticket tolto quando l'account viene disattivato. La finestra che si allarga è in
 * MatchmakingTicketTest; il ticket tolto dalla modifica del mazzo è in DeckControllerIT.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class MatchmakingControllerIT {

	private static final String PATH = "/api/v1/matchmaking/queue";

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

	@Autowired
	private ApplicationEvents events;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	private TestBattles testBattles;
	private User user;
	private Integer deck;

	@BeforeEach
	void setUp() {
		TestCards testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		TestDecks testDecks = new TestDecks(testCards,
			new TestCollections(collectionService, transactionTemplate, jdbcTemplate), ruleSetService);
		testBattles = new TestBattles(testDecks, deckService, new TestFriends(friendshipService, jdbcTemplate));
		user = testUsers.create();
		deck = testBattles.playableDeck(user);
	}

	@Test
	@DisplayName("Joining returns 201 with the ticket: WAITING, my deck, rating 1000, window 100; my rating is created")
	void join_validDeck_returnsTicketAndCreatesRating() throws Exception {
		// Act + Assert
		join(user, deck)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("WAITING"))
			.andExpect(jsonPath("$.deckId").value(deck))
			.andExpect(jsonPath("$.rating").value(1000))
			.andExpect(jsonPath("$.queuedAt").isNotEmpty())
			.andExpect(jsonPath("$.window").value(100));
		assertThat(tickets(user)).isEqualTo(1);
		assertThat(ratings(user)).isEqualTo(1);
	}

	@Test
	@DisplayName("The ticket keeps my current rating, and an existing rating row is not recreated")
	void join_existingRating_keepsIt() throws Exception {
		// Arrange: un rating già cambiato da partite precedenti
		jdbcTemplate.update("INSERT INTO player_ratings (user_id, rating, peak_rating, games_played, wins) "
			+ "VALUES (?, 1100, 1100, 3, 3)", user.getId());

		// Act + Assert
		join(user, deck)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.rating").value(1100));
		assertThat(ratings(user)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT games_played FROM player_ratings WHERE user_id = ?",
			Integer.class, user.getId())).isEqualTo(3);
	}

	@Test
	@DisplayName("A second join returns 409 ALREADY_IN_QUEUE, also with another deck; the first ticket stays")
	void join_twice_returns409() throws Exception {
		// Arrange
		Integer otherDeck = testBattles.playableDeck(user);
		join(user, deck).andExpect(status().isCreated());

		// Act + Assert
		join(user, deck)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_IN_QUEUE"));
		join(user, otherDeck)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_IN_QUEUE"));
		mockMvc.perform(as(user, get(PATH)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.deckId").value(deck));
	}

	@Test
	@DisplayName("Someone else's deck is 404, an invalid deck 422, a missing deck id 400: nobody enters the queue")
	void join_wrongDeck_rejected() throws Exception {
		// Arrange: un mazzo di un altro utente e un mio mazzo che ha perso una carta dalla collezione
		User other = testUsers.create();
		Integer otherDeck = testBattles.playableDeck(other);
		Integer brokenDeck = testBattles.playableDeck(user);
		jdbcTemplate.update("DELETE FROM collection_cards WHERE user_id = ? AND card_id = "
			+ "(SELECT card_id FROM deck_cards WHERE deck_id = ? LIMIT 1)", user.getId(), brokenDeck);

		// Act + Assert
		join(user, otherDeck)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("DECK_NOT_FOUND"));
		join(user, brokenDeck)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("DECK_INVALID"));
		mockMvc.perform(as(user, post(PATH)).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest());
		assertThat(tickets(user)).isZero();
		assertThat(ratings(user)).isZero();
	}

	@Test
	@DisplayName("Without a ticket GET returns 404 QUEUE_TICKET_NOT_FOUND; after joining it returns my ticket")
	void getMine_withAndWithoutTicket() throws Exception {
		// Act + Assert
		mockMvc.perform(as(user, get(PATH)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("QUEUE_TICKET_NOT_FOUND"));
		join(user, deck).andExpect(status().isCreated());
		mockMvc.perform(as(user, get(PATH)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("WAITING"))
			.andExpect(jsonPath("$.deckId").value(deck))
			.andExpect(jsonPath("$.window").value(100));
	}

	@Test
	@DisplayName("Leaving returns 204 and removes the ticket; leaving again is still 204; the rating stays")
	void leave_removesTicket_idempotent() throws Exception {
		// Arrange
		join(user, deck).andExpect(status().isCreated());

		// Act + Assert
		mockMvc.perform(as(user, delete(PATH))).andExpect(status().isNoContent());
		assertThat(tickets(user)).isZero();
		mockMvc.perform(as(user, get(PATH))).andExpect(status().isNotFound());
		mockMvc.perform(as(user, delete(PATH))).andExpect(status().isNoContent());
		assertThat(ratings(user)).isEqualTo(1);
	}

	@Test
	@DisplayName("Leaving removes only my ticket, not another user's")
	void leave_onlyMine() throws Exception {
		// Arrange
		User other = testUsers.create();
		join(user, deck).andExpect(status().isCreated());
		join(other, testBattles.playableDeck(other)).andExpect(status().isCreated());

		// Act
		mockMvc.perform(as(user, delete(PATH))).andExpect(status().isNoContent());

		// Assert
		assertThat(tickets(user)).isZero();
		assertThat(tickets(other)).isEqualTo(1);
	}

	@Test
	@DisplayName("Joining publishes one QueueJoinedEvent for the immediate matching attempt; a rejected join publishes none")
	void join_publishesQueueJoinedEvent() throws Exception {
		// Act
		join(user, deck).andExpect(status().isCreated());
		join(user, deck).andExpect(status().isConflict());

		// Assert: l'evento lo ascolta MatchmakingJob, spento nei test (API-49)
		assertThat(events.stream(QueueJoinedEvent.class)).containsExactly(new QueueJoinedEvent(user.getId()));
	}

	@Test
	@DisplayName("An admin disabling my account removes my ticket")
	void disable_removesTicket() throws Exception {
		// Arrange
		User admin = testUsers.createAdmin();
		join(user, deck).andExpect(status().isCreated());

		// Act
		mockMvc.perform(as(admin, post("/api/v1/admin/users/" + user.getId() + "/disable")))
			.andExpect(status().isOk());

		// Assert
		assertThat(tickets(user)).isZero();
	}

	private ResultActions join(User who, Integer deckId) throws Exception {
		return mockMvc.perform(as(who, post(PATH)).contentType(MediaType.APPLICATION_JSON)
			.content("{\"deckId\": " + deckId + "}"));
	}

	private int tickets(User who) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM matchmaking_queue WHERE user_id = ?", Integer.class,
			who.getId());
	}

	private int ratings(User who) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM player_ratings WHERE user_id = ?", Integer.class,
			who.getId());
	}

	private MockHttpServletRequestBuilder as(User who, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(who));
	}
}
