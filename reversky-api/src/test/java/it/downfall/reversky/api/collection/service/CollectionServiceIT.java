package it.downfall.reversky.api.collection.service;

import static it.downfall.reversky.api.support.TestCards.uniqueName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.entity.Faction;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.collection.dto.AvailableCardResponse;
import it.downfall.reversky.api.collection.dto.CollectionEntryResponse;
import it.downfall.reversky.api.collection.dto.CollectionFilter;
import it.downfall.reversky.api.collection.dto.CollectionSummaryResponse;
import it.downfall.reversky.api.collection.dto.RarityProgressResponse;
import it.downfall.reversky.api.collection.exception.InvalidCollectionSortException;
import it.downfall.reversky.api.collection.exception.NotEnoughCopiesException;
import it.downfall.reversky.api.common.dto.PageResponse;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestCollections;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.exception.UserNotFoundException;

/**
 * {@link CollectionService} su PostgreSQL vero (docs/collezione).
 * <p>
 * Le carte restano nel database tra un test e l'altro: le liste filtrano per un token univoco nel nome, e il
 * riepilogo si controlla su una rarità creata dal test.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class CollectionServiceIT {

	private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20);

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

	@Autowired
	private TestUsers testUsers;

	// Non serve al test: stessa configurazione degli altri IT, così Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	private TestCards testCards;

	private TestCollections testCollections;

	@BeforeEach
	void setUp() {
		testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		testCollections = new TestCollections(collectionService, transactionTemplate, jdbcTemplate);
	}

	// --- addCards e removeCards ---

	@Test
	@DisplayName("addCards creates the row for a new card and adds to the quantity of an owned card")
	void addCards_newAndOwnedCards_createAndIncrement() {
		// Arrange
		User user = testUsers.create();
		CatalogCard owned = testCards.createCreature();
		CatalogCard fresh = testCards.createCreature();
		testCollections.give(user, owned, 2);

		// Act
		transactionTemplate.executeWithoutResult(status ->
			collectionService.addCards(user.getId(), Map.of(owned.getId(), 1, fresh.getId(), 3)));

		// Assert
		assertThat(testCollections.quantity(user, owned)).isEqualTo(3);
		assertThat(testCollections.quantity(user, fresh)).isEqualTo(3);
	}

	@Test
	@DisplayName("removeCards subtracts copies and deletes the row when it reaches 0")
	void removeCards_toZero_deletesRow() {
		// Arrange
		User user = testUsers.create();
		CatalogCard kept = testCards.createCreature();
		CatalogCard gone = testCards.createCreature();
		testCollections.give(user, kept, 3);
		testCollections.give(user, gone, 2);

		// Act
		transactionTemplate.executeWithoutResult(status ->
			collectionService.removeCards(user.getId(), Map.of(kept.getId(), 1, gone.getId(), 2)));

		// Assert
		assertThat(testCollections.quantity(user, kept)).isEqualTo(2);
		Integer rows = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM collection_cards WHERE user_id = ? AND card_id = ?", Integer.class,
			user.getId(), gone.getId());
		assertThat(rows).isZero();
	}

	@Test
	@DisplayName("removeCards with too few copies of any card throws and changes nothing")
	void removeCards_notEnoughCopies_throwsAndChangesNothing() {
		// Arrange: della prima ci sono abbastanza copie, della seconda no
		User user = testUsers.create();
		CatalogCard enough = testCards.createCreature();
		CatalogCard missing = testCards.createCreature();
		testCollections.give(user, enough, 3);
		testCollections.give(user, missing, 1);

		// Act + Assert
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status ->
				collectionService.removeCards(user.getId(), Map.of(enough.getId(), 2, missing.getId(), 2))))
			.isInstanceOf(NotEnoughCopiesException.class)
			.hasMessageContaining("only 1");
		assertThat(testCollections.quantity(user, enough)).isEqualTo(3);
		assertThat(testCollections.quantity(user, missing)).isEqualTo(1);
	}

	@Test
	@DisplayName("removeCards of a card never owned throws NotEnoughCopiesException")
	void removeCards_cardNotOwned_throws() {
		// Arrange
		User user = testUsers.create();
		CatalogCard card = testCards.createCreature();

		// Act + Assert
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status ->
				collectionService.removeCards(user.getId(), Map.of(card.getId(), 1))))
			.isInstanceOf(NotEnoughCopiesException.class);
	}

	@Test
	@DisplayName("addCards and removeCards need the caller's transaction, and refuse empty maps and quantities below 1")
	void addAndRemove_misuse_throws() {
		// Arrange
		User user = testUsers.create();
		CatalogCard card = testCards.createCreature();

		// Act + Assert: senza transazione del chiamante
		assertThatThrownBy(() -> collectionService.addCards(user.getId(), Map.of(card.getId(), 1)))
			.isInstanceOf(IllegalTransactionStateException.class);
		assertThatThrownBy(() -> collectionService.removeCards(user.getId(), Map.of(card.getId(), 1)))
			.isInstanceOf(IllegalTransactionStateException.class);
		// errori di programmazione del chiamante
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status ->
				collectionService.addCards(user.getId(), Map.of())))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status ->
				collectionService.addCards(user.getId(), Map.of(card.getId(), 0))))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status ->
				collectionService.removeCards(user.getId(), Map.of(card.getId(), -1))))
			.isInstanceOf(IllegalArgumentException.class);
	}

	// --- GET /collection ---

	@Test
	@DisplayName("My collection lists every owned card with quantity, decks using it and copies reserved by my offers")
	void findMine_ownedCards_returnsQuantitiesDecksAndReserved() {
		// Arrange: 3 copie, in due mazzi, 2 offerte in uno scambio in cui tocca all'altro rispondere
		String token = TestUsers.uniqueSuffix();
		User user = testUsers.create();
		User other = testUsers.create();
		CatalogCard card = testCards.create("Card " + token, CardType.CREATURE);
		testCollections.give(user, card, 3);
		testCollections.createDeck(user, Map.of(card, 1));
		testCollections.createDeck(user, Map.of(card, 2));
		testCollections.createTradeOffer(user, other, "WAITING_RECIPIENT", user, card, 2);

		// Act
		PageResponse<CollectionEntryResponse> page = collectionService.findMine(user.getId(), byName(token), FIRST_PAGE);

		// Assert
		assertThat(page.content()).singleElement().satisfies(entry -> {
			assertThat(entry.card().id()).isEqualTo(card.getId());
			assertThat(entry.card().faction().id()).isEqualTo(testCards.faction().getId());
			assertThat(entry.quantity()).isEqualTo(3);
			assertThat(entry.usedInDecks()).isEqualTo(2);
			assertThat(entry.reservedQuantity()).isEqualTo(2);
		});
	}

	@Test
	@DisplayName("Only the offers of whoever made the current offer are reserved; closed trades reserve nothing")
	void findMine_reservedCopies_followTradeStatus() {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		User user = testUsers.create();
		User other = testUsers.create();
		CatalogCard card = testCards.create("Card " + token, CardType.CREATURE);
		testCollections.give(user, card, 5);
		// io sono il destinatario e tocca a me rispondere: la mia riga non è riservata
		testCollections.createTradeOffer(other, user, "WAITING_RECIPIENT", user, card, 1);
		// controproposta mia, tocca al richiedente: riservata
		testCollections.createTradeOffer(other, user, "WAITING_REQUESTER", user, card, 2);
		// scambi chiusi: niente
		testCollections.createTradeOffer(user, other, "ACCEPTED", user, card, 1);
		testCollections.createTradeOffer(user, other, "REJECTED", user, card, 1);

		// Act
		PageResponse<CollectionEntryResponse> page = collectionService.findMine(user.getId(), byName(token), FIRST_PAGE);

		// Assert
		assertThat(page.content()).singleElement()
			.satisfies(entry -> assertThat(entry.reservedQuantity()).isEqualTo(2));
	}

	@Test
	@DisplayName("My collection shows deactivated cards and variants, and other users' cards are not mine")
	void findMine_deactivatedAndVariants_areListed() {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		User user = testUsers.create();
		User other = testUsers.create();
		CatalogCard original = testCards.create("A original " + token, CardType.CREATURE);
		CatalogCard variant = testCards.createVariantOf(original, "B variant " + token);
		CatalogCard retired = testCards.deactivate(testCards.create("C retired " + token, CardType.CREATURE));
		testCollections.give(user, variant, 1);
		testCollections.give(user, retired, 1);
		testCollections.give(other, original, 1);

		// Act
		PageResponse<CollectionEntryResponse> page = collectionService.findMine(user.getId(), byName(token), FIRST_PAGE);

		// Assert: ordine per nome
		assertThat(page.content()).extracting(entry -> entry.card().id()).containsExactly(variant.getId(), retired.getId());
		assertThat(page.content().get(0).card().variantOfId()).isEqualTo(original.getId());
		assertThat(page.content().get(1).card().active()).isFalse();
	}

	@Test
	@DisplayName("Filters: type, rarity and faction (first or second), combined with the name")
	void findMine_filters_narrowTheList() {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		User user = testUsers.create();
		Rarity rare = testCards.createRarity();
		Faction second = testCards.createFaction();
		CatalogCard creature = testCards.create("Creature " + token, CardType.CREATURE);
		CatalogCard rareCreature = testCards.create("Rare " + token, CardType.CREATURE, rare);
		rareCreature.changeFactions(testCards.createFaction(), second);
		catalogCardRepository.save(rareCreature);
		CatalogCard spell = testCards.create("Spell " + token, CardType.SPELL);
		testCollections.give(user, creature, 1);
		testCollections.give(user, rareCreature, 1);
		testCollections.give(user, spell, 1);

		// Act
		PageResponse<CollectionEntryResponse> spells = collectionService.findMine(user.getId(),
			new CollectionFilter(CardType.SPELL, token, null, null, null), FIRST_PAGE);
		PageResponse<CollectionEntryResponse> rares = collectionService.findMine(user.getId(),
			new CollectionFilter(null, token, rare.getId(), null, null), FIRST_PAGE);
		PageResponse<CollectionEntryResponse> ofSecond = collectionService.findMine(user.getId(),
			new CollectionFilter(null, token, null, second.getId(), null), FIRST_PAGE);
		PageResponse<CollectionEntryResponse> ofTestFaction = collectionService.findMine(user.getId(),
			new CollectionFilter(null, token, null, testCards.faction().getId(), null), FIRST_PAGE);

		// Assert
		assertThat(spells.content()).extracting(entry -> entry.card().id()).containsExactly(spell.getId());
		assertThat(rares.content()).extracting(entry -> entry.card().id()).containsExactly(rareCreature.getId());
		assertThat(ofSecond.content()).extracting(entry -> entry.card().id()).containsExactly(rareCreature.getId());
		assertThat(ofTestFaction.content()).extracting(entry -> entry.card().id()).containsExactly(creature.getId());
	}

	@Test
	@DisplayName("Sort by rarity or quantity, ties by name; an unknown property is InvalidCollectionSortException")
	void findMine_sort_followsWhitelist() {
		// Arrange: Common con 1 copia ("A") e con 3 copie ("C"), Uncommon con 2 copie ("B")
		String token = TestUsers.uniqueSuffix();
		User user = testUsers.create();
		CatalogCard a = testCards.create("A " + token, CardType.CREATURE);
		CatalogCard b = testCards.create("B " + token, CardType.CREATURE, testCards.uncommon());
		CatalogCard c = testCards.create("C " + token, CardType.CREATURE);
		testCollections.give(user, a, 1);
		testCollections.give(user, b, 2);
		testCollections.give(user, c, 3);

		// Act
		PageResponse<CollectionEntryResponse> byRarity = collectionService.findMine(user.getId(), byName(token),
			PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "rarity")));
		PageResponse<CollectionEntryResponse> byQuantity = collectionService.findMine(user.getId(), byName(token),
			PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "quantity")));
		PageResponse<CollectionEntryResponse> byNameDesc = collectionService.findMine(user.getId(), byName(token),
			PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "name")));

		// Assert
		assertThat(byRarity.content()).extracting(entry -> entry.card().id()).containsExactly(b.getId(), a.getId(), c.getId());
		assertThat(byQuantity.content()).extracting(entry -> entry.card().id()).containsExactly(c.getId(), b.getId(), a.getId());
		assertThat(byNameDesc.content()).extracting(entry -> entry.card().id()).containsExactly(c.getId(), b.getId(), a.getId());
		assertThatThrownBy(() -> collectionService.findMine(user.getId(), byName(token),
				PageRequest.of(0, 20, Sort.by("atk"))))
			.isInstanceOf(InvalidCollectionSortException.class)
			.hasMessageContaining("name, rarity, type or quantity");
	}

	// --- GET /collection/summary ---

	@Test
	@DisplayName("The summary counts active originals per rarity; a variant counts as its original")
	void summary_countsActiveOriginalsPerRarity() {
		// Arrange: una rarità del test con 3 originali attivi (uno con una variante) e 1 disattivato
		User user = testUsers.create();
		Rarity rarity = testCards.createRarity();
		CatalogCard ownedDirectly = testCards.create(uniqueName("Owned"), CardType.CREATURE, rarity);
		CatalogCard ownedByVariant = testCards.create(uniqueName("Via variant"), CardType.CREATURE, rarity);
		CatalogCard variant = testCards.createVariantOf(ownedByVariant, uniqueName("Event"));
		testCards.create(uniqueName("Missing"), CardType.CREATURE, rarity);
		CatalogCard retired = testCards.deactivate(testCards.create(uniqueName("Retired"), CardType.CREATURE, rarity));
		testCollections.give(user, ownedDirectly, 2);
		testCollections.give(user, variant, 1);
		testCollections.give(user, retired, 1);

		// Act
		CollectionSummaryResponse summary = collectionService.summary(user.getId());

		// Assert
		RarityProgressResponse progress = summary.rarities().stream()
			.filter(row -> row.rarity().id().equals(rarity.getId()))
			.findFirst().orElseThrow();
		assertThat(progress.owned()).isEqualTo(2);
		assertThat(progress.total()).isEqualTo(3);
		assertThat(summary.owned()).isEqualTo(summary.rarities().stream().mapToInt(RarityProgressResponse::owned).sum());
		assertThat(summary.total()).isEqualTo(summary.rarities().stream().mapToInt(RarityProgressResponse::total).sum());
	}

	// --- GET /users/{id}/collection ---

	@Test
	@DisplayName("Another user's collection shows each card with the copies available for a trade, 0 included")
	void findOf_otherUser_returnsAvailableCopies() {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		User owner = testUsers.create();
		User other = testUsers.create();
		CatalogCard partly = testCards.create("A partly " + token, CardType.CREATURE);
		CatalogCard fully = testCards.create("B fully " + token, CardType.CREATURE);
		testCollections.give(owner, partly, 3);
		testCollections.give(owner, fully, 1);
		testCollections.createTradeOffer(owner, other, "WAITING_RECIPIENT", owner, partly, 1);
		testCollections.createTradeOffer(owner, other, "WAITING_RECIPIENT", owner, fully, 1);

		// Act
		PageResponse<AvailableCardResponse> page = collectionService.findOf(owner.getId(), byName(token), FIRST_PAGE);

		// Assert
		assertThat(page.content()).extracting(entry -> entry.card().id()).containsExactly(partly.getId(), fully.getId());
		assertThat(page.content()).extracting(AvailableCardResponse::availableQuantity).containsExactly(2, 0);
	}

	@Test
	@DisplayName("The collection of a missing or disabled user is UserNotFoundException")
	void findOf_missingOrDisabledUser_throwsNotFound() {
		// Arrange
		User disabled = testUsers.create();
		testUsers.disable(disabled);

		// Act + Assert
		assertThatThrownBy(() -> collectionService.findOf(Integer.MAX_VALUE, byName("x"), FIRST_PAGE))
			.isInstanceOf(UserNotFoundException.class);
		assertThatThrownBy(() -> collectionService.findOf(disabled.getId(), byName("x"), FIRST_PAGE))
			.isInstanceOf(UserNotFoundException.class);
	}

	private static CollectionFilter byName(String name) {
		return new CollectionFilter(null, name, null, null, null);
	}
}
