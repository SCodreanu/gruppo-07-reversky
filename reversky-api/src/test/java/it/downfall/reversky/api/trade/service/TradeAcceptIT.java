package it.downfall.reversky.api.trade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.collection.service.CollectionService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.inventory.service.InventoryService;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestCollections;
import it.downfall.reversky.api.support.TestTrades;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.trade.dto.TradeCardRequest;
import it.downfall.reversky.api.trade.dto.TradeCounterRequest;
import it.downfall.reversky.api.trade.dto.TradeRequest;
import it.downfall.reversky.api.trade.dto.TradeResponse;
import it.downfall.reversky.api.trade.entity.TradeStatus;
import it.downfall.reversky.api.trade.exception.TradeClosedException;
import it.downfall.reversky.api.trade.exception.TradeNotEnoughCopiesException;
import it.downfall.reversky.api.trade.exception.TradeNotYourTurnException;
import it.downfall.reversky.api.trade.exception.TradeUserDisabledException;
import it.downfall.reversky.api.trade.repository.TradeRepository;
import it.downfall.reversky.api.user.entity.User;

/**
 * Accettazione atomica di uno scambio su un PostgreSQL vero (API-34, docs/scambi "Accettazione"): nessuna carta persa
 * o duplicata, anche con operazioni contemporanee.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class TradeAcceptIT {

	@Autowired
	private TradeService tradeService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private CollectionService collectionService;

	@Autowired
	private InventoryService inventoryService;

	@Autowired
	private TradeRepository tradeRepository;

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

	// Non serve al test: stessa configurazione degli altri IT, così Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	private TestCards testCards;
	private TestCollections testCollections;
	private TestTrades testTrades;
	private User requester;
	private User recipient;
	private CatalogCard crawlers;
	private CatalogCard mothman;

	@BeforeEach
	void setUp() {
		testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		testCollections = new TestCollections(collectionService, transactionTemplate, jdbcTemplate);
		testTrades = new TestTrades(tradeRepository, jdbcTemplate);
		requester = testUsers.create();
		recipient = testUsers.create();
		crawlers = testCards.createCreature();
		mothman = testCards.createCreature();
		testCollections.give(requester, crawlers, 3);
		testCollections.give(recipient, mothman, 1);
	}

	@Test
	@DisplayName("Accepting moves the cards both ways and the total number of cards does not change")
	void accept_movesCardsAndKeepsTotal() {
		// Arrange: 3 Crawlers contro 1 Mothman
		UUID tradeId = propose(requester, recipient, Map.of(crawlers, 3), Map.of(mothman, 1));
		int before = total(crawlers) + total(mothman);

		// Act
		TradeResponse response = tradeService.accept(recipient.getId(), tradeId);

		// Assert
		assertThat(response.status()).isEqualTo(TradeStatus.ACCEPTED);
		assertThat(testCollections.quantity(requester, crawlers)).isZero();
		assertThat(testCollections.quantity(requester, mothman)).isEqualTo(1);
		assertThat(testCollections.quantity(recipient, crawlers)).isEqualTo(3);
		assertThat(testCollections.quantity(recipient, mothman)).isZero();
		assertThat(total(crawlers) + total(mothman)).isEqualTo(before);
		assertThat(testTrades.statusOf(tradeId)).isEqualTo(TradeStatus.ACCEPTED);
	}

	@Test
	@DisplayName("A gift is accepted: only one side moves")
	void accept_gift_movesOneSide() {
		// Arrange
		UUID tradeId = propose(requester, recipient, Map.of(crawlers, 1), Map.of());

		// Act
		tradeService.accept(recipient.getId(), tradeId);

		// Assert
		assertThat(testCollections.quantity(requester, crawlers)).isEqualTo(2);
		assertThat(testCollections.quantity(recipient, crawlers)).isEqualTo(1);
		assertThat(testCollections.quantity(recipient, mothman)).isEqualTo(1);
	}

	@Test
	@DisplayName("After a counter-proposal the requester accepts it")
	void accept_afterCounter_byRequester() {
		// Arrange
		UUID tradeId = propose(requester, recipient, Map.of(crawlers, 2), Map.of(mothman, 1));
		tradeService.counter(recipient.getId(), tradeId,
			new TradeCounterRequest(cards(Map.of(mothman, 1)), cards(Map.of(crawlers, 1))));

		// Act
		tradeService.accept(requester.getId(), tradeId);

		// Assert
		assertThat(testCollections.quantity(requester, crawlers)).isEqualTo(2);
		assertThat(testCollections.quantity(recipient, crawlers)).isEqualTo(1);
		assertThat(testCollections.quantity(requester, mothman)).isEqualTo(1);
	}

	@Test
	@DisplayName("Accepting out of turn is rejected and nothing moves")
	void accept_outOfTurn_throws() {
		UUID tradeId = propose(requester, recipient, Map.of(crawlers, 1), Map.of());

		assertThatThrownBy(() -> tradeService.accept(requester.getId(), tradeId))
			.isInstanceOf(TradeNotYourTurnException.class);
		assertThat(testCollections.quantity(requester, crawlers)).isEqualTo(3);
	}

	@Test
	@DisplayName("A second accept returns TRADE_CLOSED and moves nothing more")
	void accept_twice_secondIsRejected() {
		// Arrange
		UUID tradeId = propose(requester, recipient, Map.of(crawlers, 1), Map.of());
		tradeService.accept(recipient.getId(), tradeId);

		// Act + Assert
		assertThatThrownBy(() -> tradeService.accept(recipient.getId(), tradeId))
			.isInstanceOf(TradeClosedException.class);
		assertThat(testCollections.quantity(recipient, crawlers)).isEqualTo(1);
	}

	@Test
	@DisplayName("If the acceptor no longer has enough available copies: 422, nothing moves, the trade stays open")
	void accept_acceptorLacksCopies_keepsTradeOpen() {
		// Arrange: il destinatario offre il suo unico Mothman in un altro scambio, che glielo riserva
		UUID tradeId = propose(requester, recipient, Map.of(crawlers, 1), Map.of(mothman, 1));
		propose(recipient, testUsers.create(), Map.of(mothman, 1), Map.of());

		// Act + Assert
		assertThatThrownBy(() -> tradeService.accept(recipient.getId(), tradeId))
			.isInstanceOf(TradeNotEnoughCopiesException.class);
		assertThat(testTrades.statusOf(tradeId)).isEqualTo(TradeStatus.WAITING_RECIPIENT);
		assertThat(testCollections.quantity(requester, crawlers)).isEqualTo(3);
		assertThat(testCollections.quantity(recipient, mothman)).isEqualTo(1);
	}

	@Test
	@DisplayName("If the other participant is disabled: 422 TRADE_USER_DISABLED, nothing moves, the trade stays open")
	void accept_otherDisabled_keepsTradeOpen() {
		// Arrange
		UUID tradeId = propose(requester, recipient, Map.of(crawlers, 1), Map.of(mothman, 1));
		testUsers.disable(requester);

		// Act + Assert
		assertThatThrownBy(() -> tradeService.accept(recipient.getId(), tradeId))
			.isInstanceOf(TradeUserDisabledException.class);
		assertThat(testTrades.statusOf(tradeId)).isEqualTo(TradeStatus.WAITING_RECIPIENT);
		assertThat(testCollections.quantity(recipient, mothman)).isEqualTo(1);
	}

	@Test
	@DisplayName("Two crossed trades accepted at the same time: no deadlock and no duplicated cards")
	void accept_crossedTradesConcurrently_noDeadlockNoDuplicates() throws Exception {
		// Arrange: A propone a B e B propone ad A; poi ognuno accetta quello ricevuto, insieme
		testCollections.give(recipient, crawlers, 2);
		testCollections.give(requester, mothman, 2);
		UUID fromRequester = propose(requester, recipient, Map.of(crawlers, 1), Map.of(mothman, 1));
		UUID fromRecipient = propose(recipient, requester, Map.of(crawlers, 1), Map.of(mothman, 1));
		int before = total(crawlers) + total(mothman);

		// Act
		List<String> outcomes = runTogether(
			() -> outcome(() -> tradeService.accept(recipient.getId(), fromRequester)),
			() -> outcome(() -> tradeService.accept(requester.getId(), fromRecipient)));

		// Assert
		assertThat(outcomes).containsExactly("OK", "OK");
		assertThat(total(crawlers) + total(mothman)).isEqualTo(before);
		assertThat(testCollections.quantity(requester, crawlers)).isEqualTo(3);
		assertThat(testCollections.quantity(recipient, crawlers)).isEqualTo(2);
	}

	@Test
	@DisplayName("A trade accepted while the receiver opens a pack with the same card loses no copy")
	void accept_whileReceiverGetsSameCard_losesNothing() throws Exception {
		// Arrange: il destinatario riceverà 2 Crawlers e intanto un "pacchetto" gliene aggiunge 1
		UUID tradeId = propose(requester, recipient, Map.of(crawlers, 2), Map.of());
		CountDownLatch packLocked = new CountDownLatch(1);

		// Act: l'apertura blocca l'inventario come PackService, aspetta un po' e aggiunge la carta
		List<String> outcomes = runTogether(
			() -> {
				transactionTemplate.executeWithoutResult(status -> {
					inventoryService.lockInventories(List.of(recipient.getId()));
					packLocked.countDown();
					sleep(300);
					collectionService.addCards(recipient.getId(), Map.of(crawlers.getId(), 1));
				});
				return "OK";
			},
			() -> {
				packLocked.await(5, TimeUnit.SECONDS);
				return outcome(() -> tradeService.accept(recipient.getId(), tradeId));
			});

		// Assert
		assertThat(outcomes).containsExactly("OK", "OK");
		assertThat(testCollections.quantity(recipient, crawlers)).isEqualTo(3);
		assertThat(testCollections.quantity(requester, crawlers)).isEqualTo(1);
	}

	// --- supporto ---

	private UUID propose(User from, User to, Map<CatalogCard, Integer> give, Map<CatalogCard, Integer> receive) {
		return tradeService.propose(from.getId(), new TradeRequest(to.getId(), cards(give), cards(receive))).id();
	}

	private static List<TradeCardRequest> cards(Map<CatalogCard, Integer> side) {
		return side.entrySet().stream()
			.map(entry -> new TradeCardRequest(entry.getKey().getId(), entry.getValue()))
			.toList();
	}

	// Copie di una carta tra i due partecipanti
	private int total(CatalogCard card) {
		return testCollections.quantity(requester, card) + testCollections.quantity(recipient, card);
	}

	private static String outcome(Runnable action) {
		try {
			action.run();
			return "OK";
		} catch (RuntimeException e) {
			return e.getClass().getSimpleName();
		}
	}

	@SafeVarargs
	private static List<String> runTogether(Callable<String>... tasks) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(tasks.length);
		try {
			List<Future<String>> futures = executor.invokeAll(List.of(tasks), 30, TimeUnit.SECONDS);
			List<String> results = new ArrayList<>();
			for (Future<String> future : futures) {
				results.add(future.get());
			}
			return results;
		} finally {
			executor.shutdownNow();
		}
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
