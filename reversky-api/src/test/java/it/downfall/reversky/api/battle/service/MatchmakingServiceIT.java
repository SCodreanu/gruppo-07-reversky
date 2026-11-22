package it.downfall.reversky.api.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.battle.client.EngineClient;
import it.downfall.reversky.api.battle.client.SimulationRequest;
import it.downfall.reversky.api.battle.client.SimulationResponse;
import it.downfall.reversky.api.battle.client.SimulationResponse.PlayerResult;
import it.downfall.reversky.api.battle.exception.EngineUnavailableException;
import it.downfall.reversky.api.battle.job.MatchmakingJob;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.collection.service.CollectionService;
import it.downfall.reversky.api.common.mail.EmailSender;
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
 * Abbinamento della coda classificata con il database vero (API-49, docs/sfide, "Abbinamento" e "Anti-boosting"):
 * sfidante, rating più vicino, finestra, mazzo non più valido, anti-boosting e {@code SKIP LOCKED}. Nei test il job è
 * spento ({@code src/test/resources/config/application.properties}): qui si chiama {@link MatchmakingService}
 * direttamente, con l'engine finto. La configurazione è quella di {@code BattleControllerIT}, così Spring riusa lo
 * stesso contesto.
 * <p>
 * I ticket si scrivono con SQL per scegliere rating e attesa; la coda si svuota prima di ogni test, perché i ticket
 * lasciati da altre classi di test verrebbero abbinati anche loro.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class MatchmakingServiceIT {

	@Autowired
	private MatchmakingService matchmakingService;

	@Autowired
	private ApplicationContext applicationContext;

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

	@MockitoBean
	private EmailSender emailSender;

	@MockitoBean
	private EngineClient engineClient;

	private TestBattles testBattles;

	@BeforeEach
	void setUp() {
		TestCards testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		TestDecks testDecks = new TestDecks(testCards,
			new TestCollections(collectionService, transactionTemplate, jdbcTemplate), ruleSetService);
		testBattles = new TestBattles(testDecks, deckService, new TestFriends(friendshipService, jdbcTemplate));
		jdbcTemplate.update("DELETE FROM matchmaking_queue");

		reset(engineClient);
		when(engineClient.simulate(any())).thenAnswer(call -> challengerWins(call.getArgument(0)));
	}

	@Test
	@DisplayName("Matching is off in the tests: there is no MatchmakingJob, so nothing matches a queue test while it runs")
	void matchmakingJob_disabledInTests() {
		// Act + Assert
		assertThat(applicationContext.getBeansOfType(MatchmakingJob.class)).isEmpty();
	}

	@Test
	@DisplayName("Two tickets become one RANKED battle without invite: the older ticket is the challenger, tickets are gone")
	void matchAll_twoTickets_createsRankedBattle() {
		// Arrange: chi è entrato in coda prima ha l'id maggiore, così lo sfidante si sceglie dall'ora e non dall'id
		User later = testUsers.create();
		User older = testUsers.create();
		Integer laterDeck = enqueue(later, 1000, 5);
		Integer olderDeck = enqueue(older, 1000, 20);

		// Act
		int created = matchmakingService.matchAll();

		// Assert
		assertThat(created).isEqualTo(1);
		List<Map<String, Object>> battles = battlesOf(older);
		assertThat(battles).hasSize(1);
		Map<String, Object> battle = battles.getFirst();
		assertThat(battle.get("mode")).isEqualTo("RANKED");
		assertThat(battle.get("invite_id")).isNull();
		assertThat(battle.get("challenger_id")).isEqualTo(older.getId());
		assertThat(battle.get("opponent_id")).isEqualTo(later.getId());
		assertThat(battle.get("rule_set_id")).isEqualTo(ruleSetService.getActiveRuleSet().getId());
		assertThat(ticket(older)).isZero();
		assertThat(ticket(later)).isZero();

		// La richiesta all'engine ha i due mazzi giusti dalla parte giusta
		ArgumentCaptor<SimulationRequest> request = ArgumentCaptor.forClass(SimulationRequest.class);
		verify(engineClient, times(1)).simulate(request.capture());
		assertThat(request.getValue().challenger().userId()).isEqualTo(older.getId());
		assertThat(request.getValue().opponent().userId()).isEqualTo(later.getId());
		assertThat(request.getValue().challenger().deck()).hasSize(deckSize(olderDeck));
		assertThat(request.getValue().opponent().deck()).hasSize(deckSize(laterDeck));
	}

	@Test
	@DisplayName("The closest rating in the window wins; the third ticket stays in the queue")
	void matchAll_closestRatingWins() {
		// Arrange
		User oldest = testUsers.create();
		User far = testUsers.create();
		User close = testUsers.create();
		enqueue(oldest, 1000, 30);
		enqueue(far, 1080, 20);
		enqueue(close, 1020, 10);

		// Act
		int created = matchmakingService.matchAll();

		// Assert
		assertThat(created).isEqualTo(1);
		assertThat(battlesOf(oldest)).singleElement()
			.satisfies(battle -> assertThat(battle.get("opponent_id")).isEqualTo(close.getId()));
		assertThat(battlesOf(far)).isEmpty();
		assertThat(ticket(far)).isEqualTo(1);
	}

	@Test
	@DisplayName("With two opponents at the same distance the one waiting longer wins")
	void matchAll_sameDistance_olderOpponentWins() {
		// Arrange
		User oldest = testUsers.create();
		User newer = testUsers.create();
		User older = testUsers.create();
		enqueue(oldest, 1000, 30);
		enqueue(newer, 1050, 5);
		enqueue(older, 950, 15);

		// Act
		matchmakingService.matchAll();

		// Assert
		assertThat(battlesOf(oldest)).singleElement()
			.satisfies(battle -> assertThat(battle.get("opponent_id")).isEqualTo(older.getId()));
		assertThat(ticket(newer)).isEqualTo(1);
	}

	@Test
	@DisplayName("An opponent outside the window is not matched; after waiting longer the wider window matches it")
	void matchAll_outsideWindow_waitsForWiderWindow() {
		// Arrange: 150 punti di distanza, entrambi appena entrati (finestra 100)
		User first = testUsers.create();
		User second = testUsers.create();
		enqueue(first, 1000, 2);
		enqueue(second, 1150, 1);

		// Act + Assert
		assertThat(matchmakingService.matchAll()).isZero();
		assertThat(ticket(first)).isEqualTo(1);
		assertThat(ticket(second)).isEqualTo(1);
		verify(engineClient, never()).simulate(any());

		// Dopo 30 secondi di attesa la finestra del primo è 250
		jdbcTemplate.update("UPDATE matchmaking_queue SET queued_at = queued_at - INTERVAL '30 seconds' WHERE user_id = ?",
			first.getId());
		assertThat(matchmakingService.matchAll()).isEqualTo(1);
		assertThat(battlesOf(first)).hasSize(1);
	}

	@Test
	@DisplayName("A ticket whose deck is no longer valid is removed alone and no battle is created")
	void matchAll_invalidDeck_removesOnlyThatTicket() {
		// Arrange: l'avversario ha perso una carta del mazzo dopo l'ingresso in coda
		User valid = testUsers.create();
		User broken = testUsers.create();
		enqueue(valid, 1000, 20);
		Integer brokenDeck = enqueue(broken, 1000, 10);
		loseOneCard(broken, brokenDeck);

		// Act
		int created = matchmakingService.matchAll();

		// Assert
		assertThat(created).isZero();
		assertThat(ticket(broken)).isZero();
		assertThat(ticket(valid)).isEqualTo(1);
		assertThat(battlesOf(valid)).isEmpty();
		assertThat(battlesOf(broken)).isEmpty();
		verify(engineClient, never()).simulate(any());
	}

	@Test
	@DisplayName("The oldest ticket with an invalid deck is removed alone; the other two are matched in the same run")
	void matchAll_ownDeckInvalid_removesOwnTicketAndGoesOn() {
		// Arrange
		User broken = testUsers.create();
		User second = testUsers.create();
		User third = testUsers.create();
		Integer brokenDeck = enqueue(broken, 1000, 30);
		enqueue(second, 1000, 20);
		enqueue(third, 1000, 10);
		loseOneCard(broken, brokenDeck);

		// Act
		int created = matchmakingService.matchAll();

		// Assert
		assertThat(created).isEqualTo(1);
		assertThat(ticket(broken)).isZero();
		assertThat(battlesOf(broken)).isEmpty();
		assertThat(battlesOf(second)).singleElement()
			.satisfies(battle -> assertThat(battle.get("opponent_id")).isEqualTo(third.getId()));
	}

	@Test
	@DisplayName("An opponent with an invalid deck loses its ticket and the next opponent is tried in the same transaction")
	void matchAll_opponentDeckInvalid_triesNextOpponent() {
		// Arrange: il più vicino ha il mazzo non valido, il secondo più vicino è buono
		User first = testUsers.create();
		User broken = testUsers.create();
		User next = testUsers.create();
		enqueue(first, 1000, 30);
		Integer brokenDeck = enqueue(broken, 1000, 20);
		enqueue(next, 1050, 10);
		loseOneCard(broken, brokenDeck);

		// Act
		int created = matchmakingService.matchAll();

		// Assert: il ticket tolto e la partita creata sono nella stessa transazione, che va a buon fine
		assertThat(created).isEqualTo(1);
		assertThat(ticket(broken)).isZero();
		assertThat(battlesOf(broken)).isEmpty();
		assertThat(battlesOf(first)).singleElement()
			.satisfies(battle -> assertThat(battle.get("opponent_id")).isEqualTo(next.getId()));
	}

	@Test
	@DisplayName("Two users just matched are not matched again for 10 minutes, even if they are alone in the queue")
	void matchAll_rematchWithinTenMinutes_skipped() {
		// Arrange: una prima classificata tra i due, poi rientrano entrambi in coda
		User first = testUsers.create();
		User second = testUsers.create();
		Integer firstDeck = enqueue(first, 1000, 20);
		Integer secondDeck = enqueue(second, 1000, 10);
		assertThat(matchmakingService.matchAll()).isEqualTo(1);
		enqueue(first, firstDeck, 1000, 400);
		enqueue(second, secondDeck, 1000, 400);

		// Act + Assert: anche con la finestra al massimo restano in coda
		assertThat(matchmakingService.matchAll()).isZero();
		assertThat(ticket(first)).isEqualTo(1);
		assertThat(ticket(second)).isEqualTo(1);
		assertThat(battlesOf(first)).hasSize(1);

		// Passati 10 minuti dalla partita si possono abbinare di nuovo
		jdbcTemplate.update("UPDATE battles SET created_at = created_at - INTERVAL '11 minutes' "
			+ "WHERE challenger_id = ? OR opponent_id = ?", first.getId(), first.getId());
		assertThat(matchmakingService.matchAll()).isEqualTo(1);
		assertThat(battlesOf(first)).hasSize(2);
	}

	@Test
	@DisplayName("A ticket locked by another matching run is skipped, not waited for: nothing is matched with it")
	void matchAll_lockedTicket_skipped() throws Exception {
		// Arrange: un'altra transazione tiene bloccato il ticket del primo utente
		User locked = testUsers.create();
		User free = testUsers.create();
		enqueue(locked, 1000, 20);
		enqueue(free, 1000, 10);
		CountDownLatch lockTaken = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		int created;
		try {
			Future<?> holder = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
				jdbcTemplate.queryForList("SELECT user_id FROM matchmaking_queue WHERE user_id = ? FOR UPDATE",
					locked.getId());
				lockTaken.countDown();
				await(release);
			}));
			assertThat(lockTaken.await(10, TimeUnit.SECONDS)).isTrue();

			// Act: torna subito, senza aspettare il blocco
			created = matchmakingService.matchAll();

			release.countDown();
			holder.get(10, TimeUnit.SECONDS);
		} finally {
			release.countDown();
			executor.shutdownNow();
		}

		// Assert
		assertThat(created).isZero();
		assertThat(ticket(locked)).isEqualTo(1);
		assertThat(ticket(free)).isEqualTo(1);
		verify(engineClient, never()).simulate(any());

		// Tolto il blocco i due si abbinano
		assertThat(matchmakingService.matchAll()).isEqualTo(1);
	}

	@Test
	@DisplayName("Parallel matching runs never use the same ticket: every user is in at most one battle")
	void matchAll_parallelRuns_neverShareATicket() throws Exception {
		// Arrange: dieci ticket con lo stesso rating, ognuno abbinabile con chiunque
		List<User> users = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			User user = testUsers.create();
			users.add(user);
			enqueue(user, 1000, 30 - i);
		}
		int runs = 4;
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(runs);
		List<Future<Integer>> results = new ArrayList<>();
		try {
			for (int i = 0; i < runs; i++) {
				results.add(executor.submit(() -> {
					await(start);
					return matchmakingService.matchAll();
				}));
			}

			// Act
			start.countDown();
			int created = 0;
			for (Future<Integer> result : results) {
				created += result.get(60, TimeUnit.SECONDS);
			}

			// Assert: ogni utente è in una partita sola, o è ancora in coda
			Set<Integer> matched = new HashSet<>();
			for (User user : users) {
				List<Map<String, Object>> battles = battlesOf(user);
				assertThat(battles).hasSizeLessThanOrEqualTo(1);
				assertThat(battles.size() + ticket(user)).isEqualTo(1);
				if (!battles.isEmpty()) {
					matched.add(user.getId());
				}
			}
			assertThat(matched).hasSize(created * 2);
		} finally {
			executor.shutdownNow();
		}

		// Due ticket bloccati a vicenda nello stesso istante possono restare: il giro dopo li abbina
		matchmakingService.matchAll();
		for (User user : users) {
			assertThat(battlesOf(user)).hasSize(1);
		}
		verify(engineClient, times(5)).simulate(any());
	}

	@Test
	@DisplayName("With the engine down the battle stays without result for a retry; the tickets are gone anyway")
	void matchAll_engineUnavailable_battleWaitsForRetry() {
		// Arrange
		User first = testUsers.create();
		User second = testUsers.create();
		enqueue(first, 1000, 20);
		enqueue(second, 1000, 10);
		doThrow(new EngineUnavailableException("The game engine is not available")).when(engineClient).simulate(any());

		// Act
		int created = matchmakingService.matchAll();

		// Assert
		assertThat(created).isEqualTo(1);
		assertThat(battlesOf(first)).singleElement()
			.satisfies(battle -> assertThat(battle.get("result")).isNull());
		assertThat(ticket(first)).isZero();
		assertThat(ticket(second)).isZero();
	}

	// --- supporto ---

	// Un mazzo valido nuovo e il ticket con quel rating, entrato in coda tanti secondi fa
	private Integer enqueue(User user, int rating, int secondsAgo) {
		Integer deck = testBattles.playableDeck(user);
		enqueue(user, deck, rating, secondsAgo);
		return deck;
	}

	private void enqueue(User user, Integer deck, int rating, int secondsAgo) {
		jdbcTemplate.update("INSERT INTO player_ratings (user_id, rating, peak_rating) VALUES (?, ?, ?) "
			+ "ON CONFLICT (user_id) DO NOTHING", user.getId(), rating, rating);
		jdbcTemplate.update("INSERT INTO matchmaking_queue (user_id, deck_id, rating, queued_at) VALUES (?, ?, ?, ?)",
			user.getId(), deck, rating, Timestamp.from(Instant.now().minusSeconds(secondsAgo)));
	}

	// Una carta del mazzo esce dalla collezione: il mazzo non è più valido
	private void loseOneCard(User user, Integer deck) {
		jdbcTemplate.update("DELETE FROM collection_cards WHERE user_id = ? AND card_id = "
			+ "(SELECT card_id FROM deck_cards WHERE deck_id = ? LIMIT 1)", user.getId(), deck);
	}

	private int ticket(User user) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM matchmaking_queue WHERE user_id = ?", Integer.class,
			user.getId());
	}

	private int deckSize(Integer deck) {
		return jdbcTemplate.queryForObject("SELECT sum(quantity) FROM deck_cards WHERE deck_id = ?", Integer.class, deck);
	}

	private List<Map<String, Object>> battlesOf(User user) {
		return jdbcTemplate.queryForList("SELECT challenger_id, opponent_id, mode::text AS mode, invite_id, "
			+ "result::text AS result, rule_set_id FROM battles WHERE challenger_id = ? OR opponent_id = ? "
			+ "ORDER BY created_at", user.getId(), user.getId());
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(30, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Latch not released in time");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	private static SimulationResponse challengerWins(SimulationRequest request) {
		return new SimulationResponse("CHALLENGER_WIN", "LIFE_POINTS_ZERO", 37,
			new PlayerResult(request.challenger().userId(), 9, 14), new PlayerResult(request.opponent().userId(), 0, 11),
			request.seed(), request.rules().rulesVersion(),
			List.of(Map.of("sequence", 1, "round", 0, "type", "MATCH_STARTED", "message", "Match started")));
	}
}
