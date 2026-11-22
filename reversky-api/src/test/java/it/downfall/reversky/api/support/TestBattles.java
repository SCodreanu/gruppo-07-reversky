package it.downfall.reversky.api.support;

import java.util.List;
import java.util.Map;

import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.deck.dto.DeckCardRequest;
import it.downfall.reversky.api.deck.dto.DeckRequest;
import it.downfall.reversky.api.deck.service.DeckService;
import it.downfall.reversky.api.user.entity.User;

/**
 * Giocatori pronti per una sfida amichevole: amici e con un mazzo valido (docs/test, "Dati di test").
 * <p>
 * Come {@link TestDecks} non è un {@code @TestComponent}: si crea con
 * {@code new TestBattles(testDecks, deckService, testFriends)}.
 */
public class TestBattles {

	private final TestDecks testDecks;
	private final DeckService deckService;
	private final TestFriends testFriends;

	public TestBattles(TestDecks testDecks, DeckService deckService, TestFriends testFriends) {
		this.testDecks = testDecks;
		this.deckService = deckService;
		this.testFriends = testFriends;
	}

	/**
	 * Un mazzo valido salvato con {@link DeckService}, con le carte già nella collezione dell'utente.
	 *
	 * @return l'id del mazzo
	 */
	public Integer playableDeck(User user) {
		Map<CatalogCard, Integer> content = testDecks.validContent(user);
		List<DeckCardRequest> cards = content.entrySet().stream()
			.map(entry -> new DeckCardRequest(entry.getKey().getId(), entry.getValue()))
			.toList();
		return deckService.create(user.getId(), new DeckRequest("Deck " + TestUsers.uniqueSuffix(), cards)).id();
	}

	public void makeFriends(User a, User b) {
		testFriends.makeFriends(a, b);
	}
}
