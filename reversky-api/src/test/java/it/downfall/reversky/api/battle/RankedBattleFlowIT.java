package it.downfall.reversky.api.battle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
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
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.support.TestBattles;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestCollections;
import it.downfall.reversky.api.support.TestDecks;
import it.downfall.reversky.api.support.TestFriends;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Flusso completo della partita classificata con il database vero (parte classificata di API-57): ingresso in coda
 * via HTTP, abbinamento, Elo (API-50), dettaglio, storico e {@code GET /rankings/me} dei due giocatori, retry.
 * L'abbinamento si chiama direttamente con {@link MatchmakingService}, perché nei test il job è spento; l'engine è un
 * mock. Gli altri casi dell'abbinamento sono in {@code MatchmakingServiceIT}. La configurazione è quella di
 * {@code BattleControllerIT}, così Spring riusa lo stesso contesto.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class RankedBattleFlowIT {

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
		TestDecks testDecks = new TestDecks(testCards,
			new TestCollections(collectionService, transactionTemplate, jdbcTemplate), ruleSetService);
		testBattles = new TestBattles(testDecks, deckService, new TestFriends(friendshipService, jdbcTemplate));
		// I ticket lasciati da altre classi di test verrebbero abbinati anche loro
		jdbcTemplate.update("DELETE FROM matchmaking_queue");

		challenger = testUsers.create();
		opponent = testUsers.create();

		reset(engineClient);
		when(engineClient.simulate(any())).thenAnswer(call -> challengerWins(call.getArgument(0)));
	}

	@Test
	@DisplayName("Queue, match, simulate: both players see the battle, the Elo change and their new rating")
	void rankedBattle_fromQueueToRating() throws Exception {
		// Arrange: due giocatori nuovi (1000, K = 40); chi entra prima è lo sfidante
		joinQueue(challenger);
		joinQueue(opponent);

		// Act
		assertThat(matchmakingService.matchAll()).isEqualTo(1);

		// Assert: a pari rating E = 0,5, quindi ±20 con K = 40
		String battleId = lastRankedBattle(challenger);
		mockMvc.perform(as(challenger, get("/api/v1/battles/" + battleId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.mode").value("RANKED"))
			.andExpect(jsonPath("$.completed").value(true))
			.andExpect(jsonPath("$.mySide").value("CHALLENGER"))
			.andExpect(jsonPath("$.outcome").value("WIN"))
			.andExpect(jsonPath("$.ratingBefore").value(1000))
			.andExpect(jsonPath("$.ratingDelta").value(20))
			.andExpect(jsonPath("$.opponent.id").value(opponent.getId()));
		mockMvc.perform(as(opponent, get("/api/v1/battles/" + battleId)))
			.andExpect(jsonPath("$.mySide").value("OPPONENT"))
			.andExpect(jsonPath("$.outcome").value("LOSS"))
			.andExpect(jsonPath("$.ratingBefore").value(1000))
			.andExpect(jsonPath("$.ratingDelta").value(-20));
		assertHistory(challenger, battleId, "WIN", 20);
		assertHistory(opponent, battleId, "LOSS", -20);
		assertRanking(challenger, 1020, 1020, 1, 1, 0, 0);
		assertRanking(opponent, 980, 1000, 1, 0, 1, 0);

		// Nessuno dei due è più in coda
		mockMvc.perform(as(challenger, get("/api/v1/matchmaking/queue"))).andExpect(status().isNotFound());
		mockMvc.perform(as(opponent, get("/api/v1/matchmaking/queue"))).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("A draw uses each player's rating and games played before the battle: K 20 and K 40 give -3 and +6")
	void rankedDraw_differentK_unevenChanges() throws Exception {
		// Arrange: lo sfidante ha 1100 dopo 12 partite (K = 20), l'avversario 1000 dopo 3 (K = 40); E = 0,64 e 0,36
		jdbcTemplate.update("INSERT INTO player_ratings (user_id, rating, peak_rating, games_played, wins, losses, draws) "
			+ "VALUES (?, 1100, 1150, 12, 6, 4, 2)", challenger.getId());
		jdbcTemplate.update("INSERT INTO player_ratings (user_id, rating, peak_rating, games_played, wins, losses, draws) "
			+ "VALUES (?, 1000, 1000, 3, 1, 1, 1)", opponent.getId());
		joinQueue(challenger);
		joinQueue(opponent);
		doAnswer(call -> draw(call.getArgument(0))).when(engineClient).simulate(any());

		// Act
		assertThat(matchmakingService.matchAll()).isEqualTo(1);

		// Assert: round(20 × (0,5 − 0,64)) = −3, round(40 × (0,5 − 0,36)) = +6
		String battleId = lastRankedBattle(challenger);
		assertHistory(challenger, battleId, "DRAW", -3);
		assertHistory(opponent, battleId, "DRAW", 6);
		assertRanking(challenger, 1097, 1150, 13, 6, 4, 3);
		assertRanking(opponent, 1006, 1006, 4, 1, 1, 2);
		assertThat(ratingColumns(battleId)).containsExactly(1100, -3, 1000, 6);
	}

	@Test
	@DisplayName("Engine down at match time: no Elo yet; the retry rates from the current rating, a second retry changes nothing")
	void rankedRetry_appliesEloOnceFromCurrentRating() throws Exception {
		// Arrange: la simulazione dopo l'abbinamento fallisce
		joinQueue(challenger);
		joinQueue(opponent);
		doThrow(new EngineUnavailableException("The game engine is not available")).when(engineClient).simulate(any());
		assertThat(matchmakingService.matchAll()).isEqualTo(1);
		String battleId = lastRankedBattle(challenger);
		mockMvc.perform(as(opponent, get("/api/v1/battles/" + battleId)))
			.andExpect(jsonPath("$.completed").value(false))
			.andExpect(jsonPath("$.ratingBefore").isEmpty())
			.andExpect(jsonPath("$.ratingDelta").isEmpty());
		assertRanking(challenger, 1000, 1000, 0, 0, 0, 0);
		assertThat(ratingColumns(battleId)).containsOnlyNulls();

		// Nel frattempo il rating dello sfidante cambia: conta quello del salvataggio, non quello del ticket
		jdbcTemplate.update("UPDATE player_ratings SET rating = 1050, peak_rating = 1050 WHERE user_id = ?",
			challenger.getId());
		reset(engineClient);
		when(engineClient.simulate(any())).thenAnswer(call -> challengerWins(call.getArgument(0)));

		// Act: il retry di uno dei due completa la partita, un secondo retry la restituisce così com'è
		mockMvc.perform(as(opponent, post("/api/v1/battles/" + battleId + "/retry")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.completed").value(true))
			.andExpect(jsonPath("$.ratingBefore").value(1000))
			.andExpect(jsonPath("$.ratingDelta").value(-17));
		mockMvc.perform(as(challenger, post("/api/v1/battles/" + battleId + "/retry")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ratingBefore").value(1050))
			.andExpect(jsonPath("$.ratingDelta").value(17));

		// Assert: 1050 contro 1000 (E = 0,57), K = 40 per entrambi: +17 e −17, una volta sola
		assertRanking(challenger, 1067, 1067, 1, 1, 0, 0);
		assertRanking(opponent, 983, 1000, 1, 0, 1, 0);
		assertThat(ratingColumns(battleId)).containsExactly(1050, 17, 1000, -17);
	}

	@Test
	@DisplayName("Two overlapping retries of a ranked battle both reach the engine, but the Elo is applied once")
	void rankedRetry_overlapping_appliesEloOnce() throws Exception {
		// Arrange: una classificata rimasta senza risultato
		joinQueue(challenger);
		joinQueue(opponent);
		doThrow(new EngineUnavailableException("The game engine is not available")).when(engineClient).simulate(any());
		matchmakingService.matchAll();
		String battleId = lastRankedBattle(challenger);

		// Le due chiamate all'engine si aspettano a vicenda e rispondono con esiti diversi: vince il primo salvato
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

		// Act
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

		// Assert: una sola partita contata per ciascuno, e le variazioni scritte sono quelle applicate
		assertThat(overlapped).isTrue();
		assertThat(first.getStatus()).isEqualTo(200);
		assertThat(second.getStatus()).isEqualTo(200);
		Integer challengerDelta = JsonPath.read(first.getContentAsString(), "$.ratingDelta");
		Integer opponentDelta = JsonPath.read(second.getContentAsString(), "$.ratingDelta");
		assertThat(challengerDelta).isIn(20, -20);
		assertThat(challengerDelta).isEqualTo(-opponentDelta);
		assertThat(gamesPlayed(challenger)).isEqualTo(1);
		assertThat(gamesPlayed(opponent)).isEqualTo(1);
		assertThat(rating(challenger)).isEqualTo(1000 + challengerDelta);
		assertThat(rating(opponent)).isEqualTo(1000 + opponentDelta);
		assertThat(ratingColumns(battleId)).containsExactly(1000, challengerDelta, 1000, opponentDelta);
	}

	// --- supporto ---

	// Entra in coda via HTTP con un mazzo nuovo; il ticket si porta un secondo indietro per ogni ingresso già fatto,
	// così l'ordine di ingresso non dipende dalla risoluzione dell'orologio
	private void joinQueue(User user) throws Exception {
		Integer deck = testBattles.playableDeck(user);
		mockMvc.perform(as(user, post("/api/v1/matchmaking/queue")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"deckId\": " + deck + "}"))
			.andExpect(status().isCreated());
		jdbcTemplate.update("UPDATE matchmaking_queue SET queued_at = queued_at - INTERVAL '1 second' "
			+ "WHERE user_id <> ?", user.getId());
	}

	private String lastRankedBattle(User user) throws Exception {
		return JsonPath.read(mockMvc.perform(as(user, get("/api/v1/battles")).param("mode", "RANKED").param("size", "1"))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString(), "$.content[0].battleId");
	}

	private void assertHistory(User user, String battleId, String outcome, int ratingDelta) throws Exception {
		mockMvc.perform(as(user, get("/api/v1/battles")).param("mode", "RANKED"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].battleId").value(battleId))
			.andExpect(jsonPath("$.content[0].mode").value("RANKED"))
			.andExpect(jsonPath("$.content[0].outcome").value(outcome))
			.andExpect(jsonPath("$.content[0].ratingDelta").value(ratingDelta));
	}

	private void assertRanking(User user, int rating, int peak, int games, int wins, int losses, int draws)
			throws Exception {
		mockMvc.perform(as(user, get("/api/v1/rankings/me")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rating").value(rating))
			.andExpect(jsonPath("$.peakRating").value(peak))
			.andExpect(jsonPath("$.gamesPlayed").value(games))
			.andExpect(jsonPath("$.wins").value(wins))
			.andExpect(jsonPath("$.losses").value(losses))
			.andExpect(jsonPath("$.draws").value(draws));
	}

	private List<Integer> ratingColumns(String battleId) {
		Map<String, Object> row = jdbcTemplate.queryForMap("SELECT challenger_rating_before, challenger_rating_delta, "
			+ "opponent_rating_before, opponent_rating_delta FROM battles WHERE id = ?::uuid", battleId);
		return Arrays.asList((Integer) row.get("challenger_rating_before"),
			(Integer) row.get("challenger_rating_delta"), (Integer) row.get("opponent_rating_before"),
			(Integer) row.get("opponent_rating_delta"));
	}

	private int rating(User user) {
		return jdbcTemplate.queryForObject("SELECT rating FROM player_ratings WHERE user_id = ?", Integer.class,
			user.getId());
	}

	private int gamesPlayed(User user) {
		return jdbcTemplate.queryForObject("SELECT games_played FROM player_ratings WHERE user_id = ?", Integer.class,
			user.getId());
	}

	private MockHttpServletResponse retry(User user, String battleId) throws Exception {
		return mockMvc.perform(as(user, post("/api/v1/battles/" + battleId + "/retry"))).andReturn().getResponse();
	}

	private MockHttpServletRequestBuilder as(User who, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(who));
	}

	private static SimulationResponse challengerWins(SimulationRequest request) {
		return response("CHALLENGER_WIN", request, 9, 0);
	}

	private static SimulationResponse opponentWins(SimulationRequest request) {
		return response("OPPONENT_WIN", request, 0, 4);
	}

	private static SimulationResponse draw(SimulationRequest request) {
		return response("DRAW", request, 0, 0);
	}

	private static SimulationResponse response(String result, SimulationRequest request, int challengerLifePoints,
			int opponentLifePoints) {
		return new SimulationResponse(result, "LIFE_POINTS_ZERO", 37,
			new PlayerResult(request.challenger().userId(), challengerLifePoints, 14),
			new PlayerResult(request.opponent().userId(), opponentLifePoints, 11), request.seed(),
			request.rules().rulesVersion(),
			List.of(Map.of("sequence", 1, "round", 0, "type", "MATCH_STARTED", "message", "Match started")));
	}
}
