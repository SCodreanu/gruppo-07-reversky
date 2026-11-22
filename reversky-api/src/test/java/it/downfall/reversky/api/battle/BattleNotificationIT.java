package it.downfall.reversky.api.battle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.battle.client.EngineClient;
import it.downfall.reversky.api.battle.client.SimulationRequest;
import it.downfall.reversky.api.battle.client.SimulationResponse;
import it.downfall.reversky.api.battle.client.SimulationResponse.PlayerResult;
import it.downfall.reversky.api.battle.exception.EngineUnavailableException;
import it.downfall.reversky.api.battle.service.MatchmakingService;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.collection.service.CollectionService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.deck.service.DeckService;
import it.downfall.reversky.api.friendship.service.FriendshipService;
import it.downfall.reversky.api.notification.service.NotificationStreamService;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.support.TestBattles;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestCollections;
import it.downfall.reversky.api.support.TestDecks;
import it.downfall.reversky.api.support.TestFriends;
import it.downfall.reversky.api.support.TestNotifications;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Notifiche delle sfide via HTTP (API-68, docs/notifiche): invito ricevuto e accettato, abbinamento in classificata,
 * partita completata per entrambi (anche dopo un retry, una volta sola, mai senza risultato). La configurazione è
 * quella di {@code BattleControllerIT}, così Spring riusa lo stesso contesto.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class BattleNotificationIT {

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
	private MatchmakingService matchmakingService;

	@Autowired
	private NotificationStreamService streamService;

	@MockitoBean
	private EmailSender emailSender;

	@MockitoBean
	private EngineClient engineClient;

	private TestBattles testBattles;
	private TestNotifications testNotifications;
	private User challenger;
	private User opponent;
	private Integer challengerDeck;
	private Integer opponentDeck;

	@BeforeEach
	void setUp() {
		TestCards testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		TestDecks testDecks = new TestDecks(testCards,
			new TestCollections(collectionService, transactionTemplate, jdbcTemplate), ruleSetService);
		testBattles = new TestBattles(testDecks, deckService, new TestFriends(friendshipService, jdbcTemplate));
		testNotifications = new TestNotifications(jdbcTemplate);
		// I ticket lasciati da altre classi di test verrebbero abbinati anche loro
		jdbcTemplate.update("DELETE FROM matchmaking_queue");

		challenger = testUsers.create();
		opponent = testUsers.create();
		challengerDeck = testBattles.playableDeck(challenger);
		opponentDeck = testBattles.playableDeck(opponent);

		reset(engineClient);
		when(engineClient.simulate(any())).thenAnswer(call -> challengerWins(call.getArgument(0)));
	}

	@AfterEach
	void closeStreams() {
		streamService.disconnect(opponent.getId());
	}

	@Test
	@DisplayName("An invite notifies the opponent with the invite and the challenger; rejecting or withdrawing adds nothing")
	void invite_notifiesOpponentOnly() throws Exception {
		// Arrange
		makeFriends();

		// Act: un invito rifiutato e uno ritirato
		String rejected = invite();
		mockMvc.perform(as(opponent, post("/api/v1/battle-invites/" + rejected + "/reject")))
			.andExpect(status().isNoContent());
		String withdrawn = invite();
		mockMvc.perform(as(challenger, delete("/api/v1/battle-invites/" + withdrawn))).andExpect(status().isNoContent());

		// Assert
		assertThat(testNotifications.types(opponent)).containsExactly("BATTLE_INVITE_RECEIVED", "BATTLE_INVITE_RECEIVED");
		assertThat(testNotifications.lastPayload(opponent, "BATTLE_INVITE_RECEIVED"))
			.contains("\"inviteId\": \"" + withdrawn + "\"")
			.contains("\"userId\": " + challenger.getId())
			.contains("\"username\": \"" + challenger.getUsername() + "\"");
		assertThat(testNotifications.types(challenger)).isEmpty();
	}

	@Test
	@DisplayName("Accepting notifies the challenger, and both players get BATTLE_COMPLETED with their own outcome")
	void accept_notifiesAcceptedAndCompleted() throws Exception {
		// Arrange: l'avversario ha il canale aperto
		makeFriends();
		String inviteId = invite();
		MvcResult stream = openStream(opponent);

		// Act
		String battleId = JsonPath.read(accept(inviteId).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString(), "$.id");

		// Assert
		assertThat(testNotifications.types(challenger)).containsExactly("BATTLE_INVITE_ACCEPTED", "BATTLE_COMPLETED");
		assertThat(testNotifications.lastPayload(challenger, "BATTLE_INVITE_ACCEPTED"))
			.contains("\"inviteId\": \"" + inviteId + "\"")
			.contains("\"battleId\": \"" + battleId + "\"")
			.contains("\"username\": \"" + opponent.getUsername() + "\"");
		assertThat(testNotifications.lastPayload(challenger, "BATTLE_COMPLETED"))
			.contains("\"battleId\": \"" + battleId + "\"")
			.contains("\"mode\": \"FRIENDLY\"")
			.contains("\"outcome\": \"WIN\"")
			.contains("\"username\": \"" + opponent.getUsername() + "\"");
		assertThat(testNotifications.types(opponent)).containsExactly("BATTLE_INVITE_RECEIVED", "BATTLE_COMPLETED");
		assertThat(testNotifications.lastPayload(opponent, "BATTLE_COMPLETED"))
			.contains("\"outcome\": \"LOSS\"")
			.contains("\"username\": \"" + challenger.getUsername() + "\"");
		assertThat(stream.getResponse().getContentAsString())
			.contains("event:BATTLE_COMPLETED\n")
			.contains("\"outcome\":\"LOSS\"");
	}

	@Test
	@DisplayName("With the engine down there is no BATTLE_COMPLETED; the retry sends it once, a second retry not again")
	void engineDown_thenRetry_completedOnce() throws Exception {
		// Arrange: la simulazione fallisce, l'invito però è accettato
		makeFriends();
		doThrow(new EngineUnavailableException("The game engine is not available")).when(engineClient).simulate(any());
		String battleId = JsonPath.read(accept(invite()).andExpect(status().isServiceUnavailable())
			.andReturn().getResponse().getContentAsString(), "$.battleId");
		assertThat(testNotifications.types(challenger)).containsExactly("BATTLE_INVITE_ACCEPTED");
		assertThat(testNotifications.types(opponent)).containsExactly("BATTLE_INVITE_RECEIVED");
		reset(engineClient);
		when(engineClient.simulate(any())).thenAnswer(call -> challengerWins(call.getArgument(0)));

		// Act
		mockMvc.perform(as(opponent, post("/api/v1/battles/" + battleId + "/retry"))).andExpect(status().isOk());
		mockMvc.perform(as(challenger, post("/api/v1/battles/" + battleId + "/retry"))).andExpect(status().isOk());

		// Assert
		assertThat(testNotifications.types(challenger)).containsExactly("BATTLE_INVITE_ACCEPTED", "BATTLE_COMPLETED");
		assertThat(testNotifications.types(opponent)).containsExactly("BATTLE_INVITE_RECEIVED", "BATTLE_COMPLETED");
	}

	@Test
	@DisplayName("A ranked match notifies both players with MATCH_FOUND, then BATTLE_COMPLETED with mode RANKED")
	void rankedMatch_notifiesMatchFoundAndCompleted() throws Exception {
		// Arrange: due ticket nella coda, senza amicizia
		enqueue(challenger, challengerDeck, 20);
		enqueue(opponent, opponentDeck, 10);

		// Act
		assertThat(matchmakingService.matchAll()).isEqualTo(1);

		// Assert
		for (User player : List.of(challenger, opponent)) {
			User other = player == challenger ? opponent : challenger;
			assertThat(testNotifications.types(player)).containsExactly("MATCH_FOUND", "BATTLE_COMPLETED");
			assertThat(testNotifications.lastPayload(player, "MATCH_FOUND"))
				.contains("\"userId\": " + other.getId())
				.contains("\"username\": \"" + other.getUsername() + "\"");
			assertThat(testNotifications.lastPayload(player, "BATTLE_COMPLETED")).contains("\"mode\": \"RANKED\"");
		}
		assertThat(testNotifications.lastPayload(challenger, "BATTLE_COMPLETED")).contains("\"outcome\": \"WIN\"");
		assertThat(testNotifications.lastPayload(opponent, "BATTLE_COMPLETED")).contains("\"outcome\": \"LOSS\"");
	}

	// --- supporto ---

	// L'amicizia passa da FriendshipService, che manda le sue notifiche: si tolgono, il test guarda solo quelle delle sfide
	private void makeFriends() {
		testBattles.makeFriends(challenger, opponent);
		testNotifications.clear(challenger, opponent);
	}

	private String invite() throws Exception {
		return JsonPath.read(mockMvc.perform(as(challenger, post("/api/v1/battle-invites"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"opponentId\": " + opponent.getId() + ", \"deckId\": " + challengerDeck + "}"))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString(), "$.id");
	}

	private ResultActions accept(String inviteId) throws Exception {
		return mockMvc.perform(as(opponent, post("/api/v1/battle-invites/" + inviteId + "/accept"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"deckId\": " + opponentDeck + "}"));
	}

	private MvcResult openStream(User who) throws Exception {
		return mockMvc.perform(as(who, get("/api/v1/notifications/stream")).accept(MediaType.TEXT_EVENT_STREAM))
			.andExpect(request().asyncStarted())
			.andReturn();
	}

	// Ticket scritto con SQL, entrato in coda tanti secondi fa, come in MatchmakingServiceIT
	private void enqueue(User user, Integer deck, int secondsAgo) {
		jdbcTemplate.update("INSERT INTO player_ratings (user_id) VALUES (?) ON CONFLICT (user_id) DO NOTHING",
			user.getId());
		jdbcTemplate.update("INSERT INTO matchmaking_queue (user_id, deck_id, rating, queued_at) VALUES (?, ?, 1000, ?)",
			user.getId(), deck, Timestamp.from(Instant.now().minusSeconds(secondsAgo)));
	}

	private MockHttpServletRequestBuilder as(User who, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(who));
	}

	private static SimulationResponse challengerWins(SimulationRequest request) {
		return new SimulationResponse("CHALLENGER_WIN", "LIFE_POINTS_ZERO", 37,
			new PlayerResult(request.challenger().userId(), 9, 14), new PlayerResult(request.opponent().userId(), 0, 11),
			request.seed(), request.rules().rulesVersion(),
			List.of(Map.of("sequence", 1, "round", 0, "type", "MATCH_STARTED", "message", "Match started")));
	}
}
