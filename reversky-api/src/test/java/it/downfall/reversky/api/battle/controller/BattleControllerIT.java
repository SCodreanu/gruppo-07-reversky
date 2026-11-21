package it.downfall.reversky.api.battle.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import it.downfall.reversky.api.battle.client.EngineClient;
import it.downfall.reversky.api.battle.client.SimulationRequest;
import it.downfall.reversky.api.battle.client.SimulationResponse;
import it.downfall.reversky.api.battle.client.SimulationResponse.PlayerResult;
import it.downfall.reversky.api.battle.exception.EngineInputRejectedException;
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
 * Accettazione, simulazione, dettaglio e storico delle partite via HTTP (API-40, API-41, API-42, API-43), con
 * l'engine sostituito da un mock: qui si prova il flusso dell'API, il contratto con l'engine è in EngineClientTest.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class BattleControllerIT {

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
	private Integer challengerDeck;
	private Integer opponentDeck;

	@BeforeEach
	void setUp() {
		TestCards testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		TestDecks testDecks = new TestDecks(testCards, new TestCollections(collectionService, transactionTemplate, jdbcTemplate), ruleSetService);
		testBattles = new TestBattles(testDecks, deckService, new TestFriends(friendshipService, jdbcTemplate));

		challenger = testUsers.create();
		opponent = testUsers.create();
		testBattles.makeFriends(challenger, opponent);
		challengerDeck = testBattles.playableDeck(challenger);
		opponentDeck = testBattles.playableDeck(opponent);

		reset(engineClient);
		when(engineClient.simulate(any())).thenAnswer(call -> engineWin(call.getArgument(0)));
	}

	@Test
	@DisplayName("Accepting simulates the battle: the opponent sees a LOSS, the invite is accepted, no rating or credits")
	void accept_simulatesAndSaves() throws Exception {
		// Arrange
		String inviteId = invite();
		int creditsBefore = inventoryService.getCredits(opponent.getId());

		// Act
		String body = accept(opponent, inviteId, opponentDeck)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.mode").value("FRIENDLY"))
			.andExpect(jsonPath("$.completed").value(true))
			.andExpect(jsonPath("$.result").value("CHALLENGER_WIN"))
			.andExpect(jsonPath("$.outcome").value("LOSS"))
			.andExpect(jsonPath("$.mySide").value("OPPONENT"))
			.andExpect(jsonPath("$.endReason").value("LIFE_POINTS_ZERO"))
			.andExpect(jsonPath("$.totalRounds").value(37))
			.andExpect(jsonPath("$.rulesVersion").value(ruleSetService.getActiveRuleSet().getVersion()))
			.andExpect(jsonPath("$.me.id").value(opponent.getId()))
			.andExpect(jsonPath("$.me.finalLifePoints").value(0))
			.andExpect(jsonPath("$.opponent.username").value(challenger.getUsername()))
			.andExpect(jsonPath("$.opponent.creaturesDefeated").value(14))
			.andExpect(jsonPath("$.log[0].type").value("MATCH_STARTED"))
			.andReturn().getResponse().getContentAsString();

		// Assert
		String battleId = JsonPath.read(body, "$.id");
		assertThat(jdbcTemplate.queryForObject("SELECT status::text FROM battle_invites WHERE id = ?::uuid", String.class, inviteId))
			.isEqualTo("ACCEPTED");
		assertThat(jdbcTemplate.queryForObject(
			"SELECT challenger_rating_delta FROM battles WHERE id = ?::uuid", Integer.class, battleId)).isNull();
		assertThat(inventoryService.getCredits(opponent.getId())).isEqualTo(creditsBefore);
		mockMvc.perform(as(opponent, get("/api/v1/battle-invites")).param("box", "incoming"))
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("The engine receives the seed, the active rules and both decks with one entry per copy")
	void accept_sendsSnapshotToEngine() throws Exception {
		// Act
		accept(opponent, invite(), opponentDeck).andExpect(status().isOk());

		// Assert
		ArgumentCaptor<SimulationRequest> captor = ArgumentCaptor.forClass(SimulationRequest.class);
		verify(engineClient).simulate(captor.capture());
		SimulationRequest request = captor.getValue();
		int deckSize = ruleSetService.getActiveRuleSet().getDeckSize();
		assertThat(request.challenger().userId()).isEqualTo(challenger.getId());
		assertThat(request.opponent().userId()).isEqualTo(opponent.getId());
		assertThat(request.challenger().deck()).hasSize(deckSize);
		assertThat(request.opponent().deck()).hasSize(deckSize);
		assertThat(request.rules().startingLifePoints()).isEqualTo(ruleSetService.getActiveRuleSet().getStartingLifePoints());
		assertThat(request.factions()).isNotEmpty();
		assertThat(request.challenger().deck()).allSatisfy(card -> assertThat(card.rarityOrder()).isPositive());
	}

	@Test
	@DisplayName("Both players read the battle from their side; anyone else gets 404; the history lists it")
	void detailAndHistory_fromEachSide() throws Exception {
		// Arrange
		String battleId = battleIdOf(accept(opponent, invite(), opponentDeck));

		// Act + Assert
		mockMvc.perform(as(challenger, get("/api/v1/battles/" + battleId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.outcome").value("WIN"))
			.andExpect(jsonPath("$.mySide").value("CHALLENGER"))
			.andExpect(jsonPath("$.me.deck.length()").isNumber());
		mockMvc.perform(as(testUsers.create(), get("/api/v1/battles/" + battleId)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("BATTLE_NOT_FOUND"));

		mockMvc.perform(as(challenger, get("/api/v1/battles")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].battleId").value(battleId))
			.andExpect(jsonPath("$.content[0].opponent.id").value(opponent.getId()))
			.andExpect(jsonPath("$.content[0].outcome").value("WIN"))
			.andExpect(jsonPath("$.content[0].myFinalLifePoints").value(9))
			.andExpect(jsonPath("$.content[0].opponentFinalLifePoints").value(0))
			.andExpect(jsonPath("$.content[0].rounds").value(37))
			.andExpect(jsonPath("$.content[0].ratingDelta").isEmpty())
			.andExpect(jsonPath("$.content[0].log").doesNotExist());
		mockMvc.perform(as(opponent, get("/api/v1/battles")).param("mode", "RANKED"))
			.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	@DisplayName("With the engine down the answer is 503 with battleId and the battle waits without a result")
	void accept_engineDown_keepsBattleForRetry() throws Exception {
		// Arrange
		// doThrow e non when(...): when chiamerebbe la risposta già configurata con un argomento nullo
		doThrow(new EngineUnavailableException("The game engine is not available")).when(engineClient).simulate(any());
		String inviteId = invite();

		// Act
		String body = accept(opponent, inviteId, opponentDeck)
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.code").value("ENGINE_UNAVAILABLE"))
			.andReturn().getResponse().getContentAsString();

		// Assert
		String battleId = JsonPath.read(body, "$.battleId");
		mockMvc.perform(as(opponent, get("/api/v1/battles/" + battleId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.completed").value(false))
			.andExpect(jsonPath("$.result").isEmpty())
			.andExpect(jsonPath("$.log.length()").value(0));
		mockMvc.perform(as(challenger, get("/api/v1/battles")))
			.andExpect(jsonPath("$.content[0].completed").value(false))
			.andExpect(jsonPath("$.content[0].outcome").isEmpty());
	}

	@Test
	@DisplayName("Data rejected by the engine gives 500 ENGINE_INPUT_REJECTED with battleId, and the retry gives the same")
	void accept_engineRejectsInput_returns500WithBattleId() throws Exception {
		// Arrange
		doThrow(new EngineInputRejectedException()).when(engineClient).simulate(any());
		String inviteId = invite();

		// Act
		String body = accept(opponent, inviteId, opponentDeck)
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("ENGINE_INPUT_REJECTED"))
			.andReturn().getResponse().getContentAsString();

		// Assert: la partita resta senza risultato, e il retry manda gli stessi dati
		String battleId = JsonPath.read(body, "$.battleId");
		mockMvc.perform(as(opponent, get("/api/v1/battles/" + battleId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.completed").value(false));
		mockMvc.perform(as(challenger, post("/api/v1/battles/" + battleId + "/retry")))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("ENGINE_INPUT_REJECTED"))
			.andExpect(jsonPath("$.battleId").value(battleId));
	}

	@Test
	@DisplayName("Retry sends exactly the same battle, even after the catalog and the rules changed, and completes it")
	void retry_afterEngineDown_sendsSameBattle() throws Exception {
		// Arrange: la prima simulazione fallisce
		doThrow(new EngineUnavailableException("The game engine is not available")).when(engineClient).simulate(any());
		String battleId = JsonPath.read(accept(opponent, invite(), opponentDeck)
			.andExpect(status().isServiceUnavailable()).andReturn().getResponse().getContentAsString(), "$.battleId");
		// Nel frattempo il backoffice cambia una creatura del mazzo: la fotografia non deve cambiare
		Integer changedCard = jdbcTemplate.queryForObject("SELECT dc.card_id FROM deck_cards dc JOIN catalog_cards c "
			+ "ON c.id = dc.card_id WHERE dc.deck_id = ? AND c.type = 'CREATURE' ORDER BY dc.card_id LIMIT 1", Integer.class,
			challengerDeck);
		Integer originalAtk = jdbcTemplate.queryForObject("SELECT atk FROM catalog_cards WHERE id = ?", Integer.class, changedCard);
		jdbcTemplate.update("UPDATE catalog_cards SET atk = atk + 1 WHERE id = ?", changedCard);
		reset(engineClient);
		when(engineClient.simulate(any())).thenAnswer(call -> engineWin(call.getArgument(0)));

		// Act
		mockMvc.perform(as(challenger, post("/api/v1/battles/" + battleId + "/retry")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.completed").value(true))
			.andExpect(jsonPath("$.outcome").value("WIN"));

		// Assert: la richiesta del retry è identica a quella salvata alla preparazione
		ArgumentCaptor<SimulationRequest> captor = ArgumentCaptor.forClass(SimulationRequest.class);
		verify(engineClient).simulate(captor.capture());
		Long seed = jdbcTemplate.queryForObject("SELECT (battle_stats ->> 'seed')::bigint FROM battles WHERE id = ?::uuid",
			Long.class, battleId);
		assertThat(captor.getValue().seed()).isEqualTo(seed);
		assertThat(captor.getValue().challenger().deck())
			.filteredOn(card -> card.cardId().equals(changedCard))
			.isNotEmpty()
			.allSatisfy(card -> assertThat(card.atk()).isEqualTo(originalAtk));
	}

	@Test
	@DisplayName("Retrying a completed battle returns it without calling the engine; strangers get 404")
	void retry_completedOrStranger() throws Exception {
		// Arrange
		String battleId = battleIdOf(accept(opponent, invite(), opponentDeck));
		reset(engineClient);

		// Act + Assert
		mockMvc.perform(as(opponent, post("/api/v1/battles/" + battleId + "/retry")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.completed").value(true));
		verify(engineClient, never()).simulate(any());
		mockMvc.perform(as(testUsers.create(), post("/api/v1/battles/" + battleId + "/retry")))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("A second acceptance returns 409; the challenger cannot accept their own invite")
	void accept_twiceOrByChallenger() throws Exception {
		// Arrange
		String inviteId = invite();

		// Act + Assert
		accept(challenger, inviteId, challengerDeck).andExpect(status().isNotFound());
		accept(opponent, inviteId, opponentDeck).andExpect(status().isOk());
		accept(opponent, inviteId, opponentDeck)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("BATTLE_INVITE_ALREADY_ACCEPTED"));
	}

	@Test
	@DisplayName("An invalid deck at acceptance returns 422: the invite stays pending and no battle is created")
	void accept_invalidDeck_keepsInvitePending() throws Exception {
		// Arrange: una carta del mazzo dell'avversario esce dalla sua collezione
		String inviteId = invite();
		jdbcTemplate.update("DELETE FROM collection_cards WHERE user_id = ? AND card_id = "
			+ "(SELECT card_id FROM deck_cards WHERE deck_id = ? LIMIT 1)", opponent.getId(), opponentDeck);

		// Act + Assert
		accept(opponent, inviteId, opponentDeck)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("DECK_INVALID"));
		assertThat(jdbcTemplate.queryForObject("SELECT status::text FROM battle_invites WHERE id = ?::uuid", String.class, inviteId))
			.isEqualTo("PENDING");
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM battles WHERE invite_id = ?::uuid", Integer.class, inviteId))
			.isZero();
	}

	@Test
	@DisplayName("Players who are no longer friends cannot start the battle")
	void accept_noLongerFriends_returns403() throws Exception {
		// Arrange: SQL diretto, perché rimuovere l'amicizia dal service elimina anche gli inviti in attesa
		String inviteId = invite();
		jdbcTemplate.update("DELETE FROM friendships WHERE requester_id IN (?, ?) AND recipient_id IN (?, ?)",
			challenger.getId(), opponent.getId(), challenger.getId(), opponent.getId());

		// Act + Assert
		accept(opponent, inviteId, opponentDeck)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("NOT_FRIENDS"));
	}

	private String invite() throws Exception {
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

	private static String battleIdOf(ResultActions result) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
	}

	// Risposta finta dell'engine: vince lo sfidante 9 a 0 in 37 round
	private static SimulationResponse engineWin(SimulationRequest request) {
		return new SimulationResponse("CHALLENGER_WIN", "LIFE_POINTS_ZERO", 37,
			new PlayerResult(request.challenger().userId(), 9, 14), new PlayerResult(request.opponent().userId(), 0, 11),
			request.seed(), request.rules().rulesVersion(),
			List.of(Map.of("sequence", 1, "round", 0, "type", "MATCH_STARTED", "message", "Match started")));
	}

	private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}
}
