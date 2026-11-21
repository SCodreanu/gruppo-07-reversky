package it.downfall.reversky.api.deck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import it.downfall.reversky.api.catalog.entity.CardSubtype;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.deck.dto.DeckViolation;

/**
 * {@link DeckValidator} da solo, senza database (docs/mazzi, "Test minimi"): ogni regola nel caso valido e al limite.
 * <p>
 * Regole come il rule set {@code 1.0} e rarità come il seed {@code V9}: mazzo da 30 con 20 creature, 6 Spell,
 * 2 Hidden, 1 Oggetto e 1 Terreno; al massimo 2 Hidden SURVIVAL e 1 DAMAGE; Common 3 copie, Rare 2 copie e 5 per
 * mazzo, Legendary 1 copia e 2 per mazzo.
 */
class DeckValidatorTest {

	private static final DeckRules RULES = new DeckRules(30, 20, 6, 2, 1, 1, 2, 1);

	private final DeckValidator validator = new DeckValidator();

	private final Rarity common = rarity(1, "Common", 3, null);
	private final Rarity rare = rarity(3, "Rare", 2, 5);
	private final Rarity legendary = rarity(5, "Legendary", 1, 2);

	private int nextId = 1;

	// --- mazzo valido ---

	@Test
	@DisplayName("A deck with exactly the required composition is valid")
	void validate_validDeck_returnsNoViolations() {
		assertThat(validate(validDeck())).isEmpty();
	}

	// --- carte totali e composizione ---

	@Test
	@DisplayName("One creature less: wrong size and wrong creature count")
	void validate_oneCreatureLess_reportsSizeAndCreatures() {
		// Arrange
		Map<CatalogCard, Integer> deck = validDeck();
		CatalogCard creature = first(deck, CardType.CREATURE);
		deck.put(creature, deck.get(creature) - 1);

		// Act + Assert
		assertThat(codes(validate(deck))).containsExactly(DeckValidator.SIZE, DeckValidator.CREATURE_COUNT);
		assertThat(validate(deck).get(0).message()).isEqualTo("The deck has 29 cards, 30 are required");
		assertThat(validate(deck).get(1).message()).isEqualTo("The deck needs exactly 20 creatures, it has 19");
	}

	@Test
	@DisplayName("One card more of each category is reported with its own code")
	void validate_oneMoreOfEachCategory_reportsEachCode() {
		assertThat(codes(validate(withExtra(CardType.CREATURE, common, null))))
			.containsExactly(DeckValidator.SIZE, DeckValidator.CREATURE_COUNT);
		assertThat(codes(validate(withExtra(CardType.SPELL, common, CardSubtype.BURN))))
			.containsExactly(DeckValidator.SIZE, DeckValidator.SPELL_COUNT);
		assertThat(codes(validate(withExtra(CardType.ITEM, rare, null))))
			.containsExactly(DeckValidator.SIZE, DeckValidator.ITEM_COUNT);
		assertThat(codes(validate(withExtra(CardType.TERRAIN, rare, null))))
			.containsExactly(DeckValidator.SIZE, DeckValidator.TERRAIN_COUNT);
		// Una terza Hidden SURVIVAL supera anche il massimo del sottotipo
		assertThat(codes(validate(withExtra(CardType.HIDDEN, common, CardSubtype.SURVIVAL))))
			.containsExactly(DeckValidator.SIZE, DeckValidator.HIDDEN_COUNT, DeckValidator.HIDDEN_SUBTYPE_LIMIT);
	}

	@Test
	@DisplayName("One card less of each special category is reported with its own code")
	void validate_oneLessOfEachSpecialCategory_reportsEachCode() {
		for (CardType type : List.of(CardType.SPELL, CardType.HIDDEN, CardType.ITEM, CardType.TERRAIN)) {
			// Arrange
			Map<CatalogCard, Integer> deck = validDeck();
			CatalogCard card = first(deck, type);
			if (deck.get(card) == 1) {
				deck.remove(card);
			} else {
				deck.put(card, deck.get(card) - 1);
			}

			// Act + Assert
			assertThat(codes(validate(deck)))
				.as(type.name())
				.containsExactly(DeckValidator.SIZE, countCode(type));
		}
	}

	@Test
	@DisplayName("A category swapped for another keeps the size but reports both counts")
	void validate_spellInsteadOfCreature_reportsBothCounts() {
		// Arrange
		Map<CatalogCard, Integer> deck = validDeck();
		CatalogCard creature = first(deck, CardType.CREATURE);
		deck.put(creature, deck.get(creature) - 1);
		deck.put(card(CardType.SPELL, common, CardSubtype.DAMAGE), 1);

		// Act + Assert
		assertThat(codes(validate(deck)))
			.containsExactly(DeckValidator.CREATURE_COUNT, DeckValidator.SPELL_COUNT);
	}

	// --- Hidden per sottotipo ---

	@Test
	@DisplayName("One DAMAGE Hidden is allowed, two are not")
	void validate_hiddenDamage_atAndOverLimit() {
		// Arrange: le due Hidden SURVIVAL del mazzo valido diventano DAMAGE, una alla volta
		Map<CatalogCard, Integer> oneDamage = withHidden(CardSubtype.SURVIVAL, CardSubtype.DAMAGE);
		Map<CatalogCard, Integer> twoDamage = withHidden(CardSubtype.DAMAGE, CardSubtype.DAMAGE);

		// Act + Assert
		assertThat(validate(oneDamage)).isEmpty();
		List<DeckViolation> violations = validate(twoDamage);
		assertThat(codes(violations)).containsExactly(DeckValidator.HIDDEN_SUBTYPE_LIMIT);
		assertThat(violations.get(0).message()).isEqualTo("Too many DAMAGE Hidden cards (2/1)");
	}

	// --- copie ---

	@Test
	@DisplayName("Copies up to the rarity limit are allowed, one more is not")
	void validate_copies_atAndOverRarityLimit() {
		// Arrange: una creatura da 3 copie sale a 4, un'altra scende a 2 per tenere il totale
		Map<CatalogCard, Integer> deck = validDeck();
		List<CatalogCard> creatures = all(deck, CardType.CREATURE);
		deck.put(creatures.get(0), 4);
		deck.put(creatures.get(1), 2);

		// Act
		List<DeckViolation> violations = validate(deck);

		// Assert
		assertThat(codes(violations)).containsExactly(DeckValidator.COPY_LIMIT);
		assertThat(violations.get(0).cardId()).isEqualTo(creatures.get(0).getId());
		assertThat(violations.get(0).message()).isEqualTo("Too many copies of '" + creatures.get(0).getName() + "' (4/3)");
	}

	@Test
	@DisplayName("A copy limit lower than the rarity wins; a null copy limit uses the rarity limit")
	void validate_cardCopyLimit_overridesRarity() {
		// Arrange: una Spell con copy_limit 1 sostituisce una delle Spell da 3 copie
		Map<CatalogCard, Integer> deck = validDeck();
		CatalogCard spell = first(deck, CardType.SPELL);
		CatalogCard burn = card(CardType.SPELL, common, CardSubtype.BURN);
		ReflectionTestUtils.setField(burn, "copyLimit", 1);
		deck.put(spell, deck.get(spell) - 2);
		deck.put(burn, 2);

		// Act + Assert
		assertThat(codes(validate(deck))).containsExactly(DeckValidator.COPY_LIMIT);
		deck.put(burn, 1);
		deck.put(spell, deck.get(spell) + 1);
		assertThat(validate(deck)).isEmpty();
	}

	@Test
	@DisplayName("A variant adds up with its original, under the original's limit")
	void validate_variantAndOriginal_copiesAddUp() {
		// Arrange: 2 copie dell'originale e 2 della variante fanno 4 copie della stessa carta Common
		Map<CatalogCard, Integer> deck = validDeck();
		List<CatalogCard> creatures = all(deck, CardType.CREATURE);
		CatalogCard original = creatures.get(0);
		CatalogCard variant = variantOf(original);
		deck.put(original, 2);
		deck.put(variant, 2);
		deck.put(creatures.get(1), 2);

		// Act
		List<DeckViolation> violations = validate(deck);

		// Assert: la violazione indica l'originale
		assertThat(codes(violations)).containsExactly(DeckValidator.COPY_LIMIT);
		assertThat(violations.get(0).cardId()).isEqualTo(original.getId());
		assertThat(violations.get(0).message()).endsWith("(4/3)");
	}

	// --- bilanciamento ---

	@Test
	@DisplayName("Two different Legendary cards are allowed, three are not")
	void validate_legendary_atAndOverDeckMaximum() {
		// Arrange
		Map<CatalogCard, Integer> twoLegendary = withCreaturesOf(legendary, 2);
		Map<CatalogCard, Integer> threeLegendary = withCreaturesOf(legendary, 3);

		// Act + Assert
		assertThat(validate(twoLegendary)).isEmpty();
		List<DeckViolation> violations = validate(threeLegendary);
		assertThat(codes(violations)).containsExactly(DeckValidator.BALANCE_LIMIT);
		assertThat(violations.get(0).message()).isEqualTo("Too many Legendary cards (3/2)");
		assertThat(violations.get(0).cardId()).isNull();
	}

	@Test
	@DisplayName("The Terrain and the Item count among the Rare cards")
	void validate_rareMaximum_countsTerrainAndItem() {
		// Arrange: Terreno + Oggetto + 3 creature Rare = 5 Rare (ok); con 4 creature Rare sono 6
		Map<CatalogCard, Integer> fiveRares = withCreaturesOf(rare, 3);
		Map<CatalogCard, Integer> sixRares = withCreaturesOf(rare, 4);

		// Act + Assert
		assertThat(validate(fiveRares)).isEmpty();
		assertThat(validate(sixRares).get(0).message()).isEqualTo("Too many Rare cards (6/5)");
	}

	// --- carte disattivate ---

	@Test
	@DisplayName("A deactivated card makes the deck invalid until it is reactivated")
	void validate_inactiveCard_reportsUntilReactivated() {
		// Arrange
		Map<CatalogCard, Integer> deck = validDeck();
		CatalogCard creature = first(deck, CardType.CREATURE);
		creature.deactivate();

		// Act + Assert
		List<DeckViolation> violations = validate(deck);
		assertThat(codes(violations)).containsExactly(DeckValidator.INACTIVE_CARD);
		assertThat(violations.get(0).cardId()).isEqualTo(creature.getId());
		creature.activate();
		assertThat(validate(deck)).isEmpty();
	}

	// --- possesso ---

	@Test
	@DisplayName("A card that is not owned at all is reported")
	void validate_cardNotOwned_reportsNotOwned() {
		// Arrange
		Map<CatalogCard, Integer> deck = validDeck();
		CatalogCard creature = first(deck, CardType.CREATURE);
		Map<Integer, Integer> available = ownedAll(deck);
		available.remove(creature.getId());

		// Act
		List<DeckViolation> violations = validator.validate(RULES, deck, available);

		// Assert
		assertThat(codes(violations)).containsExactly(DeckValidator.NOT_OWNED);
		assertThat(violations.get(0).cardId()).isEqualTo(creature.getId());
		assertThat(violations.get(0).message())
			.isEqualTo("You have 0 available copies of '" + creature.getName() + "', the deck contains 3");
	}

	@Test
	@DisplayName("Available copies equal to the deck quantity are enough, one less is not")
	void validate_availableCopies_atAndBelowQuantity() {
		// Arrange: 3 copie nel mazzo, prima 3 disponibili, poi 2 (es. una riservata da uno scambio)
		Map<CatalogCard, Integer> deck = validDeck();
		CatalogCard creature = first(deck, CardType.CREATURE);
		Map<Integer, Integer> available = ownedAll(deck);

		// Act + Assert
		assertThat(validator.validate(RULES, deck, available)).isEmpty();
		available.put(creature.getId(), 1);
		assertThat(validator.validate(RULES, deck, available).get(0).message())
			.isEqualTo("You have 1 available copy of '" + creature.getName() + "', the deck contains 3");
	}

	@Test
	@DisplayName("A variant needs its own copies: owning the original is not enough")
	void validate_variantNotOwned_reportsVariant() {
		// Arrange
		Map<CatalogCard, Integer> deck = validDeck();
		List<CatalogCard> creatures = all(deck, CardType.CREATURE);
		CatalogCard variant = variantOf(creatures.get(0));
		deck.put(creatures.get(0), 2);
		deck.put(variant, 1);
		Map<Integer, Integer> available = ownedAll(deck);
		available.remove(variant.getId());
		available.put(creatures.get(0).getId(), 3);

		// Act + Assert
		assertThat(validator.validate(RULES, deck, available))
			.extracting(DeckViolation::code, DeckViolation::cardId)
			.containsExactly(tuple(DeckValidator.NOT_OWNED, variant.getId()));
	}

	// --- più errori ---

	@Test
	@DisplayName("A deck with several problems returns all of them, in a fixed order")
	void validate_severalProblems_returnsAll() {
		// Arrange: una carta disattivata, 4 copie di una Common, nessun Terreno
		Map<CatalogCard, Integer> deck = validDeck();
		List<CatalogCard> creatures = all(deck, CardType.CREATURE);
		deck.put(creatures.get(0), 4);
		deck.put(creatures.get(1), 2);
		creatures.get(2).deactivate();
		deck.remove(first(deck, CardType.TERRAIN));

		// Act + Assert
		assertThat(codes(validate(deck))).containsExactly(
			DeckValidator.SIZE, DeckValidator.TERRAIN_COUNT, DeckValidator.COPY_LIMIT, DeckValidator.INACTIVE_CARD);
	}

	@Test
	@DisplayName("An empty deck reports the size and every category")
	void validate_emptyDeck_reportsEverything() {
		assertThat(codes(validate(Map.of()))).containsExactly(DeckValidator.SIZE,
			DeckValidator.CREATURE_COUNT, DeckValidator.SPELL_COUNT, DeckValidator.HIDDEN_COUNT,
			DeckValidator.ITEM_COUNT, DeckValidator.TERRAIN_COUNT);
	}

	// --- costruzione dei mazzi di prova ---

	// Le regole senza il possesso: l'utente ha tutte le copie che servono
	private List<DeckViolation> validate(Map<CatalogCard, Integer> deck) {
		return validator.validate(RULES, deck, ownedAll(deck));
	}

	private static Map<Integer, Integer> ownedAll(Map<CatalogCard, Integer> deck) {
		Map<Integer, Integer> available = new HashMap<>();
		deck.forEach((card, quantity) -> available.put(card.getId(), quantity));
		return available;
	}

	// 20 creature (6 da 3 copie e 1 da 2), 2 Spell da 3, 2 Hidden SURVIVAL da 1, 1 Oggetto e 1 Terreno Rare
	private Map<CatalogCard, Integer> validDeck() {
		Map<CatalogCard, Integer> deck = new LinkedHashMap<>();
		for (int i = 0; i < 6; i++) {
			deck.put(card(CardType.CREATURE, common, null), 3);
		}
		deck.put(card(CardType.CREATURE, common, null), 2);
		deck.put(card(CardType.SPELL, common, CardSubtype.BURN), 3);
		deck.put(card(CardType.SPELL, common, CardSubtype.SHIELD), 3);
		deck.put(card(CardType.HIDDEN, common, CardSubtype.SURVIVAL), 1);
		deck.put(card(CardType.HIDDEN, common, CardSubtype.SURVIVAL), 1);
		deck.put(card(CardType.ITEM, rare, null), 1);
		deck.put(card(CardType.TERRAIN, rare, null), 1);
		return deck;
	}

	private Map<CatalogCard, Integer> withExtra(CardType type, Rarity rarity, CardSubtype subtype) {
		Map<CatalogCard, Integer> deck = validDeck();
		deck.put(card(type, rarity, subtype), 1);
		return deck;
	}

	private Map<CatalogCard, Integer> withHidden(CardSubtype first, CardSubtype second) {
		Map<CatalogCard, Integer> deck = validDeck();
		all(deck, CardType.HIDDEN).forEach(deck::remove);
		deck.put(card(CardType.HIDDEN, common, first), 1);
		deck.put(card(CardType.HIDDEN, common, second), 1);
		return deck;
	}

	// Sostituisce "count" copie Common con "count" creature diverse della rarità, una copia ciascuna
	private Map<CatalogCard, Integer> withCreaturesOf(Rarity rarity, int count) {
		Map<CatalogCard, Integer> deck = validDeck();
		List<CatalogCard> creatures = all(deck, CardType.CREATURE);
		int removed = 0;
		for (CatalogCard creature : creatures) {
			while (removed < count && deck.getOrDefault(creature, 0) > 0) {
				deck.merge(creature, -1, Integer::sum);
				removed++;
			}
			if (deck.get(creature) == 0) {
				deck.remove(creature);
			}
		}
		for (int i = 0; i < count; i++) {
			deck.put(card(CardType.CREATURE, rarity, null), 1);
		}
		return deck;
	}

	private CatalogCard card(CardType type, Rarity rarity, CardSubtype subtype) {
		boolean creature = type == CardType.CREATURE;
		int id = nextId++;
		CatalogCard card = new CatalogCard(null, rarity, null, null, type + " " + id, type, subtype,
			creature ? null : "TEST_EFFECT", "/images/test.webp", null, null, creature ? 5 : null, creature ? 5 : null, null);
		ReflectionTestUtils.setField(card, "id", id);
		return card;
	}

	private CatalogCard variantOf(CatalogCard original) {
		int id = nextId++;
		CatalogCard variant = new CatalogCard(original, original.getRarity(), null, null, original.getName(),
			original.getCardType(), original.getCardSubtype(), original.getEffectCode(), "/images/variant.webp", null, null,
			original.getAtk(), original.getDef(), original.getCopyLimit());
		ReflectionTestUtils.setField(variant, "id", id);
		return variant;
	}

	private static Rarity rarity(int id, String name, int deckLimit, Integer maxPerDeck) {
		Rarity rarity = new Rarity(name, deckLimit, maxPerDeck, id);
		ReflectionTestUtils.setField(rarity, "id", id);
		return rarity;
	}

	private static CatalogCard first(Map<CatalogCard, Integer> deck, CardType type) {
		return all(deck, type).get(0);
	}

	private static List<CatalogCard> all(Map<CatalogCard, Integer> deck, CardType type) {
		return deck.keySet().stream().filter(card -> card.getCardType() == type).toList();
	}

	private static List<String> codes(List<DeckViolation> violations) {
		return violations.stream().map(DeckViolation::code).toList();
	}

	private static String countCode(CardType type) {
		return switch (type) {
			case CREATURE -> DeckValidator.CREATURE_COUNT;
			case SPELL -> DeckValidator.SPELL_COUNT;
			case HIDDEN -> DeckValidator.HIDDEN_COUNT;
			case ITEM -> DeckValidator.ITEM_COUNT;
			case TERRAIN -> DeckValidator.TERRAIN_COUNT;
		};
	}
}
