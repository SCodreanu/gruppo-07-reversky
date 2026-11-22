package it.downfall.reversky.api.battle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
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
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.collection.service.CollectionService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.deck.service.DeckService;
import it.downfall.reversky.api.friendship.service.FriendshipService;
import it.downfall.reversky.api.inventory.service.InventoryService;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.support.TestBattles;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestCollections;
import it.downfall.reversky.api.support.TestDecks;
import it.downfall.reversky.api.support.TestFriends;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Flusso completo della sfida amichevole via HTTP con il database vero (API-57, #127): amicizia, invito,
 * accettazione, dettaglio e storico dei due giocatori, senza crediti né rating. L'engine è un mock (docs/test,
 * "Test con l'engine"); il JSON scambiato con l'engine vero lo controlla {@code EngineContractTest}, e la prova sullo
 * stack completo è in docs/sfide.
 * <p>
 * I singoli passi e gli errori sono già in {@code BattleInviteControllerIT} e {@code BattleControllerIT}; qui si
 * controlla il percorso intero e la voce di docs/test sui retry sovrapposti, che non devono scrivere la partita due
 * volte. La configurazione è quella di {@code BattleControllerIT}, così Spring riusa lo stesso contesto.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class FriendlyBattleFlowIT {

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
	private InventoryService inventoryService;

	@MockitoBean
	private EmailSender emailSender;

	@MockitoBean
	private EngineClient engineClient;

	private TestBattles testBattles;
	private User challenger;
	private User opponent;

	@BeforeEach
	void setUp() {
		TestCards testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		TestDecks testDecks = new TestDecks(testCards, new TestCollections(collectionService, transactionTemplate, jdbcTemplate), ruleSetService);
		testBattles = new TestBattles(testDecks, deckService, new TestFriends(friendshipService, jdbcTemplate));

		challenger = testUsers.create();
		opponent = testUsers.create();

		reset(engineClient);
		when(engineClient.simulate(any())).thenAnswer(call -> challengerWins(call.getArgument(0)));
	}

	@Test
	@DisplayName("Friend request, invite, accept, detail and history for both players: no credits, no rating, no cards spent")
	void friendlyBattle_fromFriendRequestToHistory() throws Exception {
		// Arrange: l'amicizia passa da HTTP, i mazzi dal service dei mazzi (la loro creazione via HTTP è in DeckControllerIT)
		mockMvc.perform(as(challenger, post("/api/v1/friends/requests")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\": \"" + opponent.getUsername() + "\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PENDING"));
		mockMvc.perform(as(opponent, post("/api/v1/friends/requests/" + challenger.getId() + "/accept")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACCEPTED"));
		mockMvc.perform(as(challenger, get("/api/v1/friends")))
			.andExpect(jsonPath("$[?(@.userId == " + opponent.getId() + ")].username").value(opponent.getUsername()));
		Integer challengerDeck = testBattles.playableDeck(challenger);
		Integer opponentDeck = testBattles.playableDeck(opponent);
		Map<User, Integer> creditsBefore = credits();
		Map<User, Integer> movementsBefore = creditMovements();
		Map<User, Integer> cardsBefore = ownedCards();

		// Act
		String inviteId = invite(challengerDeck);
		mockMvc.perform(as(opponent, get("/api/v1/battle-invites")).param("box", "incoming"))
			.andExpect(jsonPath("$[0].id").value(inviteId))
			.andExpect(jsonPath("$[0].challenger.id").value(challenger.getId()));
		String battleId = JsonPath.read(accept(opponent, inviteId, opponentDeck)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.completed").value(true))
			.andExpect(jsonPath("$.outcome").value("LOSS"))
			.andReturn().getResponse().getContentAsString(), "$.id");

		// Assert: ognuno legge la partita e lo storico dalla sua parte
		mockMvc.perform(as(challenger, get("/api/v1/battles/" + battleId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.mode").value("FRIENDLY"))
			.andExpect(jsonPath("$.outcome").value("WIN"))
			.andExpect(jsonPath("$.ratingBefore").isEmpty())
			.andExpect(jsonPath("$.ratingDelta").isEmpty())
			.andExpect(jsonPath("$.mySide").value("CHALLENGER"))
			.andExpect(jsonPath("$.opponent.id").value(opponent.getId()))
			.andExpect(jsonPath("$.log[0].type").value("MATCH_STARTED"));
		assertHistory(challenger, battleId, "WIN");
		assertHistory(opponent, battleId, "LOSS");

		// Nessun invito resta in attesa
		mockMvc.perform(as(opponent, get("/api/v1/battle-invites")).param("box", "incoming"))
			.andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(as(challenger, get("/api/v1/battle-invites")).param("box", "outgoing"))
			.andExpect(jsonPath("$.length()").value(0));

		// Le amichevoli non danno crediti né rating e non consumano carte
		assertThat(credits()).isEqualTo(creditsBefore);
		assertThat(creditMovements()).isEqualTo(movementsBefore);
		assertThat(ownedCards()).isEqualTo(cardsBefore);
		assertThat(ratingRows()).isZero();
		assertNoRatingColumns(battleId);
		verify(engineClient, times(1)).simulate(any());
	}

	@Test
	@DisplayName("Two retries at the same time both reach the engine, but the battle is written once and both see it")
	void retry_overlapping_writesBattleOnce() throws Exception {
		// Arrange: la prima simulazione fallisce, la partita resta senza risultato
		testBattles.makeFriends(challenger, opponent);
		Integer challengerDeck = testBattles.playableDeck(challenger);
		Integer opponentDeck = testBattles.playableDeck(opponent);
		doThrow(new EngineUnavailableException("The game engine is not available")).when(engineClient).simulate(any());
		String battleId = JsonPath.read(accept(opponent, invite(challengerDeck), opponentDeck)
			.andExpect(status().isServiceUnavailable())
			.andReturn().getResponse().getContentAsString(), "$.battleId");

		// Le due chiamate all'engine si aspettano a vicenda, così i retry sono davvero sovrapposti. Le risposte sono
		// diverse apposta (l'engine vero darebbe la stessa): così si vede se la seconda sovrascrive la prima.
		CountDownLatch bothInEngine = new CountDownLatch(2);
		AtomicBoolean overlapped = new AtomicBoolean(true);
		AtomicInteger calls = new AtomicInteger();
		reset(engineClient);
		when(engineClient.simulate(any())).thenAnswer(call -> {
			bothInEngine.countDown();
			if (!bothInEngine.await(10, TimeUnit.SECONDS)) {
				overlapped.set(false);
			}
			SimulationRequest request = call.getArgument(0);
			return calls.getAndIncrement() == 0 ? challengerWins(request) : opponentWins(request);
		});
		Map<User, Integer> creditsBefore = credits();

		// Act: un retry per giocatore, nello stesso momento
		MockHttpServletResponse first;
		MockHttpServletResponse second;
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<MockHttpServletResponse> byChallenger = executor.submit(() -> retry(challenger, battleId));
			Future<MockHttpServletResponse> byOpponent = executor.submit(() -> retry(opponent, battleId));
			first = byChallenger.get(30, TimeUnit.SECONDS);
			second = byOpponent.get(30, TimeUnit.SECONDS);
		} finally {
			executor.shutdownNow();
		}

		// Assert: entrambi hanno simulato, ma vale solo il risultato salvato per primo e tutti e due lo vedono
		assertThat(overlapped).as("both retries were in the engine at the same time").isTrue();
		verify(engineClient, times(2)).simulate(any());
		assertThat(first.getStatus()).isEqualTo(200);
		assertThat(second.getStatus()).isEqualTo(200);
		String stored = jdbcTemplate.queryForObject("SELECT result::text FROM battles WHERE id = ?::uuid", String.class,
			battleId);
		Integer storedRounds = jdbcTemplate.queryForObject(
			"SELECT (battle_stats ->> 'totalRounds')::int FROM battles WHERE id = ?::uuid", Integer.class, battleId);
		for (MockHttpServletResponse response : List.of(first, second)) {
			String body = response.getContentAsString();
			assertThat((String) JsonPath.read(body, "$.id")).isEqualTo(battleId);
			assertThat((String) JsonPath.read(body, "$.result")).isEqualTo(stored);
			assertThat((Integer) JsonPath.read(body, "$.totalRounds")).isEqualTo(storedRounds);
		}
		assertThat(credits()).isEqualTo(creditsBefore);
		assertThat(ratingRows()).isZero();
		assertNoRatingColumns(battleId);
	}

	private void assertHistory(User user, String battleId, String outcome) throws Exception {
		mockMvc.perform(as(user, get("/api/v1/battles")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].battleId").value(battleId))
			.andExpect(jsonPath("$.content[0].mode").value("FRIENDLY"))
			.andExpect(jsonPath("$.content[0].completed").value(true))
			.andExpect(jsonPath("$.content[0].outcome").value(outcome))
			.andExpect(jsonPath("$.content[0].ratingDelta").isEmpty());
	}

	private void assertNoRatingColumns(String battleId) {
		Integer withRating = jdbcTemplate.queryForObject("""
			SELECT COUNT(*) FROM battles
			WHERE id = ?::uuid
				AND (challenger_rating_before IS NOT NULL OR challenger_rating_delta IS NOT NULL
					OR opponent_rating_before IS NOT NULL OR opponent_rating_delta IS NOT NULL)
			""", Integer.class, battleId);
		assertThat(withRating).isZero();
	}

	private Map<User, Integer> credits() {
		return Map.of(challenger, inventoryService.getCredits(challenger.getId()),
			opponent, inventoryService.getCredits(opponent.getId()));
	}

	private Map<User, Integer> creditMovements() {
		return Map.of(challenger, countRows("credit_transactions", challenger), opponent, countRows("credit_transactions", opponent));
	}

	private Map<User, Integer> ownedCards() {
		return Map.of(challenger, sumOwnedCards(challenger), opponent, sumOwnedCards(opponent));
	}

	private int sumOwnedCards(User user) {
		return jdbcTemplate.queryForObject("SELECT COALESCE(SUM(quantity), 0) FROM collection_cards WHERE user_id = ?",
			Integer.class, user.getId());
	}

	private int ratingRows() {
		return countRows("player_ratings", challenger) + countRows("player_ratings", opponent);
	}

	private int countRows(String table, User user) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE user_id = ?", Integer.class,
			user.getId());
	}

	private String invite(Integer challengerDeck) throws Exception {
		String body = mockMvc.perform(as(challenger, post("/api/v1/battle-invites")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"opponentId\": %d, \"deckId\": %d}".formatted(opponent.getId(), challengerDeck)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private ResultActions accept(User user, String inviteId, Integer deckId) throws Exception {
		return mockMvc.perform(as(user, post("/api/v1/battle-invites/" + inviteId + "/accept"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"deckId\": " + deckId + "}"));
	}

	private MockHttpServletResponse retry(User user, String battleId) throws Exception {
		return mockMvc.perform(as(user, post("/api/v1/battles/" + battleId + "/retry"))).andReturn().getResponse();
	}

	// Risposte finte dell'engine: vince lo sfidante 9 a 0 in 37 round, oppure l'avversario 0 a 4 in 12 round
	private static SimulationResponse challengerWins(SimulationRequest request) {
		return new SimulationResponse("CHALLENGER_WIN", "LIFE_POINTS_ZERO", 37,
			new PlayerResult(request.challenger().userId(), 9, 14), new PlayerResult(request.opponent().userId(), 0, 11),
			request.seed(), request.rules().rulesVersion(),
			List.of(Map.of("sequence", 1, "round", 0, "type", "MATCH_STARTED", "message", "Match started")));
	}

	private static SimulationResponse opponentWins(SimulationRequest request) {
		return new SimulationResponse("OPPONENT_WIN", "LIFE_POINTS_ZERO", 12,
			new PlayerResult(request.challenger().userId(), 0, 3), new PlayerResult(request.opponent().userId(), 4, 6),
			request.seed(), request.rules().rulesVersion(),
			List.of(Map.of("sequence", 1, "round", 0, "type", "MATCH_STARTED", "message", "Match started")));
	}

	private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}
}
