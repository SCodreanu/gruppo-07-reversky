package it.downfall.reversky.api.trade.controller;

import static it.downfall.reversky.api.support.TestCards.uniqueName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.collection.service.CollectionService;
import it.downfall.reversky.api.common.exception.BusinessException;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestCollections;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.trade.dto.TradeCardRequest;
import it.downfall.reversky.api.trade.dto.TradeRequest;
import it.downfall.reversky.api.trade.service.TradeService;
import it.downfall.reversky.api.user.entity.User;

/**
 * Proposta e controproposta via HTTP (API-33, docs/scambi): copie riservate, turni e regole delle offerte.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class TradeProposalControllerIT {

	private static final String BASE_PATH = "/api/v1/trades";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private TradeService tradeService;

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

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	private TestCards testCards;
	private TestCollections testCollections;
	private User requester;
	private User recipient;
	private CatalogCard crawlers;
	private CatalogCard mothman;

	@BeforeEach
	void setUp() {
		testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		testCollections = new TestCollections(collectionService, transactionTemplate, jdbcTemplate);
		requester = testUsers.create();
		recipient = testUsers.create();
		crawlers = testCards.createCreature();
		mothman = testCards.createCreature();
		testCollections.give(requester, crawlers, 3);
		testCollections.give(recipient, mothman, 1);
	}

	// --- proposta ---

	@Test
	@DisplayName("A valid proposal returns 201 with both sides from my point of view and reserves my copies")
	void propose_valid_returns201AndReserves() throws Exception {
		// Act
		propose(requester, recipient.getId(), give(crawlers, 2), give(mothman, 1))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("WAITING_RECIPIENT"))
			.andExpect(jsonPath("$.myTurn").value(false))
			.andExpect(jsonPath("$.other.id").value(recipient.getId()))
			.andExpect(jsonPath("$.give[0].card.id").value(crawlers.getId()))
			.andExpect(jsonPath("$.give[0].card.name").value(crawlers.getName()))
			.andExpect(jsonPath("$.give[0].quantity").value(2))
			.andExpect(jsonPath("$.receive[0].card.id").value(mothman.getId()))
			.andExpect(jsonPath("$.receive[0].quantity").value(1));

		// Assert: le copie restano nella collezione ma 2 sono riservate
		assertThat(testCollections.quantity(requester, crawlers)).isEqualTo(3);
		mockMvc.perform(as(requester, get("/api/v1/collection")))
			.andExpect(jsonPath("$.content[?(@.card.id == " + crawlers.getId() + ")].reservedQuantity", hasItem(2)));
	}

	@Test
	@DisplayName("The recipient sees the same trade the other way round, with its turn")
	void propose_recipientView_isMirrored() throws Exception {
		// Arrange
		UUID tradeId = proposeOk(requester, recipient.getId(), give(crawlers, 2), give(mothman, 1));

		// Act + Assert
		mockMvc.perform(as(recipient, get(BASE_PATH + "/" + tradeId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.myTurn").value(true))
			.andExpect(jsonPath("$.give[0].card.id").value(mothman.getId()))
			.andExpect(jsonPath("$.receive[0].card.id").value(crawlers.getId()))
			.andExpect(jsonPath("$.receive[0].quantity").value(2));
		mockMvc.perform(as(recipient, get(BASE_PATH).param("box", "incoming")))
			.andExpect(jsonPath("$.content[0].receive[0].quantity").value(2));
	}

	@Test
	@DisplayName("A gift (nothing asked) and a request (nothing given) are both accepted")
	void propose_oneEmptySide_isAccepted() throws Exception {
		propose(requester, recipient.getId(), give(crawlers, 1), List.of()).andExpect(status().isCreated());
		propose(requester, recipient.getId(), List.of(), give(mothman, 1)).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("A trade without any card returns 422 TRADE_EMPTY")
	void propose_noCards_returns422() throws Exception {
		propose(requester, recipient.getId(), List.of(), List.of())
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("TRADE_EMPTY"));
	}

	@Test
	@DisplayName("Trading with myself returns 422 TRADE_SELF")
	void propose_self_returns422() throws Exception {
		propose(requester, requester.getId(), give(crawlers, 1), List.of())
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("TRADE_SELF"));
	}

	@Test
	@DisplayName("A missing or disabled recipient returns 404 USER_NOT_FOUND")
	void propose_missingOrDisabledRecipient_returns404() throws Exception {
		// Arrange
		User disabled = testUsers.create();
		testUsers.disable(disabled);

		// Act + Assert
		propose(requester, disabled.getId(), give(crawlers, 1), List.of())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
		propose(requester, Integer.MAX_VALUE, give(crawlers, 1), List.of())
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("The same card on both sides returns 422 TRADE_SAME_CARD_BOTH_SIDES")
	void propose_sameCardBothSides_returns422() throws Exception {
		testCollections.give(recipient, crawlers, 1);

		propose(requester, recipient.getId(), give(crawlers, 2), give(crawlers, 1))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("TRADE_SAME_CARD_BOTH_SIDES"));
	}

	@Test
	@DisplayName("An unknown card returns 422 TRADE_CARD_NOT_FOUND")
	void propose_unknownCard_returns422() throws Exception {
		propose(requester, recipient.getId(), List.of(new TradeCardRequest(Integer.MAX_VALUE, 1)), List.of())
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("TRADE_CARD_NOT_FOUND"));
	}

	@Test
	@DisplayName("A repeated card on one side or a quantity below 1 returns 400 VALIDATION_FAILED")
	void propose_badShape_returns400() throws Exception {
		propose(requester, recipient.getId(),
				List.of(new TradeCardRequest(crawlers.getId(), 1), new TradeCardRequest(crawlers.getId(), 1)), List.of())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		propose(requester, recipient.getId(), give(crawlers, 0), List.of())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	@DisplayName("With 3 copies, offering 2 in one trade and 2 more in a second returns 422 TRADE_NOT_ENOUGH_COPIES")
	void propose_overReserving_returns422() throws Exception {
		// Arrange
		User other = testUsers.create();
		proposeOk(requester, recipient.getId(), give(crawlers, 2), List.of());

		// Act + Assert
		propose(requester, other.getId(), give(crawlers, 2), List.of())
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("TRADE_NOT_ENOUGH_COPIES"));
		propose(requester, other.getId(), give(crawlers, 1), List.of()).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("Asking for more copies than the recipient owns returns 422 TRADE_NOT_OWNED")
	void propose_recipientDoesNotOwn_returns422() throws Exception {
		propose(requester, recipient.getId(), List.of(), give(mothman, 2))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("TRADE_NOT_OWNED"));
	}

	@Test
	@DisplayName("A deactivated card can be traded")
	void propose_deactivatedCard_isAccepted() throws Exception {
		testCards.deactivate(crawlers);

		propose(requester, recipient.getId(), give(crawlers, 1), List.of()).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("Rejecting a trade frees the reserved copies right away")
	void reject_freesReservedCopies() throws Exception {
		// Arrange
		UUID tradeId = proposeOk(requester, recipient.getId(), give(crawlers, 3), List.of());
		User other = testUsers.create();
		propose(requester, other.getId(), give(crawlers, 1), List.of()).andExpect(status().isUnprocessableContent());

		// Act
		mockMvc.perform(as(recipient, post(BASE_PATH + "/" + tradeId + "/reject"))).andExpect(status().isOk());

		// Assert
		propose(requester, other.getId(), give(crawlers, 3), List.of()).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("A deck using reserved copies is not valid until the trade closes")
	void propose_deckUsesReservedCopies_isInvalidUntilClosed() throws Exception {
		// Arrange: un mazzo che usa tutte e 3 le copie; il resto della composizione non conta, si guarda solo
		// DECK_NOT_OWNED
		Integer deckId = testCollections.createDeck(requester, Map.of(crawlers, 3));
		UUID tradeId = proposeOk(requester, recipient.getId(), give(crawlers, 1), List.of());

		// Act + Assert
		mockMvc.perform(as(requester, get("/api/v1/decks/" + deckId)))
			.andExpect(jsonPath("$.violations[*].code", hasItem("DECK_NOT_OWNED")));
		mockMvc.perform(as(recipient, post(BASE_PATH + "/" + tradeId + "/reject"))).andExpect(status().isOk());
		mockMvc.perform(as(requester, get("/api/v1/decks/" + deckId)))
			.andExpect(jsonPath("$.violations[?(@.code == 'DECK_NOT_OWNED')]").isEmpty());
	}

	@Test
	@DisplayName("Two simultaneous proposals on the same copies: only one succeeds")
	void propose_concurrentOnSameCopies_onlyOneSucceeds() throws Exception {
		// Arrange: 3 copie, due proposte da 2 a due utenti diversi
		User first = testUsers.create();
		User second = testUsers.create();
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		List<Callable<String>> tasks = List.of(
			() -> proposeConcurrently(start, first),
			() -> proposeConcurrently(start, second));

		// Act
		List<Future<String>> results = new ArrayList<>();
		for (Callable<String> task : tasks) {
			results.add(executor.submit(task));
		}
		start.countDown();
		List<String> outcomes = new ArrayList<>();
		for (Future<String> result : results) {
			outcomes.add(result.get());
		}
		executor.shutdown();

		// Assert
		assertThat(outcomes).containsExactlyInAnyOrder("OK", "TRADE_NOT_ENOUGH_COPIES");
	}

	// --- controproposta ---

	@Test
	@DisplayName("A counter-proposal replaces the offers, passes the turn and moves the reservation")
	void counter_replacesOffersAndPassesTurn() throws Exception {
		// Arrange: il richiedente dà 2 Crawlers e chiede Mothman
		testCollections.give(recipient, crawlers, 1);
		UUID tradeId = proposeOk(requester, recipient.getId(), give(crawlers, 2), give(mothman, 1));
		CatalogCard spell = testCards.create(uniqueName("Spell"), CardType.SPELL);
		testCollections.give(recipient, spell, 1);

		// Act: il destinatario chiede ancora Crawlers ma 3, e dà la Spell invece di Mothman
		counter(recipient, tradeId, give(spell, 1), give(crawlers, 3))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("WAITING_REQUESTER"))
			.andExpect(jsonPath("$.myTurn").value(false))
			.andExpect(jsonPath("$.give.length()").value(1))
			.andExpect(jsonPath("$.give[0].card.id").value(spell.getId()))
			.andExpect(jsonPath("$.receive[0].card.id").value(crawlers.getId()))
			.andExpect(jsonPath("$.receive[0].quantity").value(3));

		// Assert: le copie del richiedente non sono più riservate, quelle del destinatario sì
		assertThat(collectionService.getAvailableCopies(requester.getId(), List.of(crawlers.getId())))
			.containsEntry(crawlers.getId(), 3);
		assertThat(collectionService.getAvailableCopies(recipient.getId(), List.of(spell.getId())))
			.containsEntry(spell.getId(), 0);
		mockMvc.perform(as(requester, get(BASE_PATH).param("box", "incoming")))
			.andExpect(jsonPath("$.content[0].id").value(tradeId.toString()))
			.andExpect(jsonPath("$.content[0].myTurn").value(true));
	}

	@Test
	@DisplayName("A counter-proposal out of turn returns 409 TRADE_NOT_YOUR_TURN")
	void counter_outOfTurn_returns409() throws Exception {
		UUID tradeId = proposeOk(requester, recipient.getId(), give(crawlers, 1), List.of());

		counter(requester, tradeId, give(crawlers, 2), List.of())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("TRADE_NOT_YOUR_TURN"));
	}

	@Test
	@DisplayName("A counter-proposal on a closed trade returns 409 TRADE_CLOSED, from a stranger 404")
	void counter_closedOrStranger_isRejected() throws Exception {
		// Arrange
		UUID tradeId = proposeOk(requester, recipient.getId(), give(crawlers, 1), List.of());

		// Act + Assert
		counter(testUsers.create(), tradeId, List.of(), give(crawlers, 1)).andExpect(status().isNotFound());
		mockMvc.perform(as(requester, post(BASE_PATH + "/" + tradeId + "/reject"))).andExpect(status().isOk());
		counter(recipient, tradeId, give(mothman, 1), List.of())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("TRADE_CLOSED"));
	}

	@Test
	@DisplayName("A counter-proposal checks my available copies like a proposal")
	void counter_notEnoughCopies_returns422() throws Exception {
		UUID tradeId = proposeOk(requester, recipient.getId(), give(crawlers, 1), List.of());

		counter(recipient, tradeId, give(mothman, 2), List.of())
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("TRADE_NOT_ENOUGH_COPIES"));
	}

	// --- accettazione (API-34; i casi limite e la concorrenza sono in TradeAcceptIT) ---

	@Test
	@DisplayName("Accepting via HTTP returns 200 ACCEPTED, moves the cards, and a second accept returns 409")
	void accept_viaHttp_movesCardsOnce() throws Exception {
		// Arrange
		UUID tradeId = proposeOk(requester, recipient.getId(), give(crawlers, 2), give(mothman, 1));

		// Act + Assert
		mockMvc.perform(as(recipient, post(BASE_PATH + "/" + tradeId + "/accept")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACCEPTED"))
			.andExpect(jsonPath("$.myTurn").value(false));
		assertThat(testCollections.quantity(recipient, crawlers)).isEqualTo(2);
		assertThat(testCollections.quantity(requester, mothman)).isEqualTo(1);
		mockMvc.perform(as(recipient, post(BASE_PATH + "/" + tradeId + "/accept")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("TRADE_CLOSED"));
	}

	@Test
	@DisplayName("Accepting with the other player disabled returns 422 TRADE_USER_DISABLED")
	void accept_otherDisabled_returns422() throws Exception {
		UUID tradeId = proposeOk(requester, recipient.getId(), give(crawlers, 1), List.of());
		testUsers.disable(requester);

		mockMvc.perform(as(recipient, post(BASE_PATH + "/" + tradeId + "/accept")))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("TRADE_USER_DISABLED"));
	}

	// --- supporto ---

	private String proposeConcurrently(CountDownLatch start, User target) throws InterruptedException {
		start.await();
		try {
			tradeService.propose(requester.getId(), new TradeRequest(target.getId(), give(crawlers, 2), List.of()));
			return "OK";
		} catch (BusinessException e) {
			return e.getCode();
		}
	}

	private UUID proposeOk(User from, Integer to, List<TradeCardRequest> give, List<TradeCardRequest> receive)
			throws Exception {
		String body = propose(from, to, give, receive)
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return UUID.fromString(JsonPath.read(body, "$.id"));
	}

	private ResultActions propose(User from, Integer to, List<TradeCardRequest> give, List<TradeCardRequest> receive)
			throws Exception {
		String json = "{\"recipientId\": " + to + ", \"give\": " + toJson(give) + ", \"receive\": " + toJson(receive) + "}";
		return mockMvc.perform(as(from, post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions counter(User from, UUID tradeId, List<TradeCardRequest> give, List<TradeCardRequest> receive)
			throws Exception {
		String json = "{\"give\": " + toJson(give) + ", \"receive\": " + toJson(receive) + "}";
		return mockMvc.perform(as(from, post(BASE_PATH + "/" + tradeId + "/counter"))
			.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static List<TradeCardRequest> give(CatalogCard card, int quantity) {
		return List.of(new TradeCardRequest(card.getId(), quantity));
	}

	private static String toJson(List<TradeCardRequest> side) {
		return side.stream()
			.map(card -> "{\"cardId\": " + card.cardId() + ", \"quantity\": " + card.quantity() + "}")
			.reduce((a, b) -> a + ", " + b)
			.map(cards -> "[" + cards + "]")
			.orElse("[]");
	}

	private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}
}
