package it.downfall.reversky.api.support;

import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.collection.service.CollectionService;
import it.downfall.reversky.api.user.entity.User;

/**
 * Helper per i test della collezione (docs/collezione).
 * <p>
 * Come {@link TestFriends} non è un {@code @TestComponent}: si crea nel test con
 * {@code new TestCollections(collectionService, transactionTemplate, jdbcTemplate)}, così {@code @Import} resta
 * quello degli altri IT e Spring riusa lo stesso contesto.
 * <p>
 * Le carte passano per {@link CollectionService}, dentro una transazione come farebbe il chiamante (apertura di un
 * pacchetto o scambio). Mazzi e scambi si scrivono con SQL finché non esistono le entità {@code Deck} e
 * {@code Trade}: servono a verificare {@code usedInDecks} e {@code reservedQuantity}.
 */
public class TestCollections {

	private final CollectionService collectionService;
	private final TransactionTemplate transactionTemplate;
	private final JdbcTemplate jdbcTemplate;

	public TestCollections(CollectionService collectionService, TransactionTemplate transactionTemplate,
			JdbcTemplate jdbcTemplate) {
		this.collectionService = collectionService;
		this.transactionTemplate = transactionTemplate;
		this.jdbcTemplate = jdbcTemplate;
	}

	/** Aggiunge copie di una carta alla collezione dell'utente, con {@link CollectionService#addCards}. */
	public void give(User user, CatalogCard card, int quantity) {
		transactionTemplate.executeWithoutResult(
			status -> collectionService.addCards(user.getId(), Map.of(card.getId(), quantity)));
	}

	/** Copie possedute, lette con SQL; 0 se la riga non c'è. */
	public int quantity(User user, CatalogCard card) {
		Integer quantity = jdbcTemplate.query(
			"SELECT quantity FROM collection_cards WHERE user_id = ? AND card_id = ?",
			rs -> rs.next() ? rs.getInt(1) : 0, user.getId(), card.getId());
		return quantity == null ? 0 : quantity;
	}

	/**
	 * Crea un mazzo dell'utente con le carte date.
	 *
	 * @return l'id del mazzo
	 */
	public Integer createDeck(User user, Map<CatalogCard, Integer> cards) {
		Integer deckId = jdbcTemplate.queryForObject(
			"INSERT INTO decks (user_id, name) VALUES (?, ?) RETURNING id",
			Integer.class, user.getId(), "Deck " + TestUsers.uniqueSuffix());
		cards.forEach((card, quantity) -> jdbcTemplate.update(
			"INSERT INTO deck_cards (deck_id, card_id, quantity) VALUES (?, ?, ?)", deckId, card.getId(), quantity));
		return deckId;
	}

	/**
	 * Crea uno scambio con lo stato dato e una riga di offerta di {@code offerer}.
	 *
	 * @param status {@code WAITING_RECIPIENT}, {@code WAITING_REQUESTER}, {@code ACCEPTED} o {@code REJECTED}
	 * @return l'id dello scambio
	 */
	public UUID createTradeOffer(User requester, User recipient, String status, User offerer, CatalogCard card,
			int quantity) {
		UUID tradeId = UUID.randomUUID();
		jdbcTemplate.update("""
			INSERT INTO trades (id, requester_id, recipient_id, status)
			VALUES (?, ?, ?, CAST(? AS trade_status))
			""", tradeId, requester.getId(), recipient.getId(), status);
		jdbcTemplate.update("INSERT INTO trade_offers (trade_id, user_id, card_id, quantity) VALUES (?, ?, ?, ?)",
			tradeId, offerer.getId(), card.getId(), quantity);
		return tradeId;
	}
}
