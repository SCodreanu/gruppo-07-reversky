package it.downfall.reversky.api.trade.entity;

import static it.downfall.reversky.api.support.TestCards.uniqueName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestTrades;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.trade.repository.TradeRepository;
import it.downfall.reversky.api.user.entity.User;

/**
 * Offerte di uno scambio su un PostgreSQL vero (API-32): righe aggiornate al loro posto, mai svuotate e reinserite.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class TradeOfferIT {

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private TradeRepository tradeRepository;

	@Autowired
	private CatalogCardRepository catalogCardRepository;

	@Autowired
	private RarityRepository rarityRepository;

	@Autowired
	private FactionRepository factionRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TransactionTemplate transactionTemplate;

	// Non serve al test: stessa configurazione degli altri IT, così Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	private TestTrades testTrades;
	private User requester;
	private User recipient;
	private CatalogCard kept;
	private CatalogCard dropped;
	private CatalogCard added;

	@BeforeEach
	void setUp() {
		TestCards testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		testTrades = new TestTrades(tradeRepository, jdbcTemplate);
		requester = testUsers.create();
		recipient = testUsers.create();
		kept = testCards.createCreature();
		dropped = testCards.createCreature();
		added = testCards.create(uniqueName("Spell"), CardType.SPELL);
	}

	@Test
	@DisplayName("The first offers are saved with the giver of each card")
	void replaceOffers_firstTime_savesBothSides() {
		// Arrange
		UUID tradeId = testTrades.create(requester, recipient);

		// Act
		replace(tradeId, requester, Map.of(kept, 3), Map.of(dropped, 1));

		// Assert: user_id è chi cede la carta
		assertThat(rows(tradeId)).containsExactlyInAnyOrder(
			row(requester, kept, 3),
			row(recipient, dropped, 1));
	}

	@Test
	@DisplayName("A counter-proposal keeping a card updates it in place, drops and adds the others without a key error")
	void replaceOffers_counterKeepingACard_updatesInPlace() {
		// Arrange: il richiedente dà 3x kept e chiede 1x dropped
		UUID tradeId = testTrades.create(requester, recipient);
		replace(tradeId, requester, Map.of(kept, 3), Map.of(dropped, 1));

		// Act: il destinatario controproponde: chiede ancora kept ma 2 copie, dà added invece di dropped
		replace(tradeId, recipient, Map.of(added, 1), Map.of(kept, 2));

		// Assert
		assertThat(rows(tradeId)).containsExactlyInAnyOrder(
			row(requester, kept, 2),
			row(recipient, added, 1));
	}

	@Test
	@DisplayName("Offers can only be given by one of the two participants")
	void replaceOffers_stranger_isRejected() {
		// Arrange
		UUID tradeId = testTrades.create(requester, recipient);
		User stranger = testUsers.create();

		// Act + Assert
		assertThatThrownBy(() -> replace(tradeId, stranger, Map.of(kept, 1), Map.of()))
			.isInstanceOf(IllegalArgumentException.class);
		assertThat(rows(tradeId)).isEmpty();
	}

	@Test
	@DisplayName("A quantity below 1 is rejected before anything is saved")
	void replaceOffers_zeroQuantity_isRejected() {
		// Arrange
		UUID tradeId = testTrades.create(requester, recipient);

		// Act + Assert
		assertThatThrownBy(() -> replace(tradeId, requester, Map.of(kept, 0), Map.of()))
			.isInstanceOf(IllegalArgumentException.class);
		assertThat(rows(tradeId)).isEmpty();
	}

	@Test
	@DisplayName("offeredBy returns only the cards that participant gives")
	void offeredBy_returnsOneSide() {
		// Arrange
		UUID tradeId = testTrades.create(requester, recipient);
		replace(tradeId, requester, Map.of(kept, 3), Map.of(dropped, 1));

		// Act
		Map<Integer, Integer> fromRequester = transactionTemplate.execute(
			status -> tradeRepository.findById(tradeId).orElseThrow().offeredBy(requester.getId()));

		// Assert
		assertThat(fromRequester).containsExactly(Map.entry(kept.getId(), 3));
	}

	// Una transazione come quella del service: carica, sostituisce, salva al commit
	private void replace(UUID tradeId, User giver, Map<CatalogCard, Integer> given, Map<CatalogCard, Integer> otherGiven) {
		transactionTemplate.executeWithoutResult(status -> {
			Trade trade = tradeRepository.findById(tradeId).orElseThrow();
			trade.replaceOffers(giver.getId(), given, otherGiven);
		});
	}

	private List<String> rows(UUID tradeId) {
		return jdbcTemplate.query("SELECT user_id, card_id, quantity FROM trade_offers WHERE trade_id = ?",
			(rs, rowNum) -> rs.getInt("user_id") + ":" + rs.getInt("card_id") + "x" + rs.getInt("quantity"), tradeId);
	}

	private static String row(User giver, CatalogCard card, int quantity) {
		return giver.getId() + ":" + card.getId() + "x" + quantity;
	}
}
