package it.downfall.reversky.engine.balance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.support.TestFactions;

class BalanceDeckBuilderTest {

	private static final int SEEDS = 200;

	private static BalanceDeckBuilder builder;

	@BeforeAll
	static void loadCatalog() throws IOException {
		builder = new BalanceDeckBuilder(CatalogCsv.load(CatalogCsv.DEFAULT_PATH));
	}

	@ParameterizedTest(name = "fazione {0}")
	@ValueSource(ints = {TestFactions.WOUND, TestFactions.INVITATION, TestFactions.ARCHIVE, TestFactions.DREAM})
	@DisplayName("Ogni mazzo rispetta la composizione e i limiti dell'API (reversky-api/docs/mazzi)")
	void build_everyDeckIsLegal(int factionId) {
		for (long seed = 0; seed < SEEDS; seed++) {
			List<CardDefinition> deck = builder.build(factionId, new Random(seed));

			assertThat(deck).hasSize(30);
			assertThat(count(deck, CardType.CREATURE)).isEqualTo(20);
			assertThat(count(deck, CardType.SPELL)).isEqualTo(6);
			assertThat(count(deck, CardType.HIDDEN)).isEqualTo(2);
			assertThat(count(deck, CardType.TERRAIN)).isEqualTo(1);
			assertThat(count(deck, CardType.ITEM)).isEqualTo(1);
			assertThat(deck).filteredOn(card -> card.type() != CardType.SPELL && card.type() != CardType.HIDDEN)
					.allSatisfy(card -> assertThat(card.factions()).contains(factionId));
			assertCopyLimits(deck);
			assertRarityCaps(deck);
			assertThat(deck).filteredOn(card -> card.subtype() == CardSubtype.DAMAGE && card.type() == CardType.HIDDEN)
					.hasSizeLessThanOrEqualTo(1);
		}
	}

	@Test
	@DisplayName("Stesso seed, stesso mazzo; seed diverso, mazzo diverso")
	void build_sameSeedSameDeck() {
		List<CardDefinition> first = builder.build(TestFactions.WOUND, new Random(7));
		List<CardDefinition> again = builder.build(TestFactions.WOUND, new Random(7));
		List<CardDefinition> other = builder.build(TestFactions.WOUND, new Random(8));

		assertThat(again).isEqualTo(first);
		assertThat(other).isNotEqualTo(first);
	}

	@Test
	@DisplayName("Le Mutant entrano nei mazzi di entrambe le loro fazioni")
	void build_mutantsInBothFactions() {
		assertThat(appearsIn("The Rootbound Doppelganger", TestFactions.INVITATION)).isTrue();
		assertThat(appearsIn("The Rootbound Doppelganger", TestFactions.DREAM)).isTrue();
		assertThat(appearsIn("The Stitched Corridor", TestFactions.WOUND)).isTrue();
		assertThat(appearsIn("The Stitched Corridor", TestFactions.ARCHIVE)).isTrue();
	}

	@Test
	@DisplayName("Le copie sono ammesse: una Common può comparire tre volte")
	void build_copiesAllowed() {
		boolean threeCopies = false;
		for (long seed = 0; seed < SEEDS && !threeCopies; seed++) {
			threeCopies = copies(builder.build(TestFactions.DREAM, new Random(seed))).containsValue(3L);
		}

		assertThat(threeCopies).isTrue();
	}

	private static boolean appearsIn(String name, int factionId) {
		for (long seed = 0; seed < SEEDS; seed++) {
			if (builder.build(factionId, new Random(seed)).stream().anyMatch(card -> card.name().equals(name))) {
				return true;
			}
		}
		return false;
	}

	private static long count(List<CardDefinition> deck, CardType type) {
		return deck.stream().filter(card -> card.type() == type).count();
	}

	private static Map<String, Long> copies(List<CardDefinition> deck) {
		return deck.stream().collect(Collectors.groupingBy(CardDefinition::name, Collectors.counting()));
	}

	/** Copie per rarità di Q-02: Common 3, Uncommon 2 (anche Spell e Hidden), Rare 2, le altre 1. */
	private static void assertCopyLimits(List<CardDefinition> deck) {
		Map<String, CardDefinition> byName = deck.stream()
				.collect(Collectors.toMap(CardDefinition::name, Function.identity(), (a, b) -> a));
		copies(deck).forEach((name, copies) -> {
			int limit = switch (byName.get(name).rarity()) {
				case "Common" -> 3;
				case "Uncommon", "Rare" -> 2;
				default -> 1;
			};
			assertThat(copies).as(name).isLessThanOrEqualTo(limit);
		});
	}

	/** Massimi per rarità di Q-03: Legendary 2, Mythic 2, Mutant 1, Rare 5 contando Terreno e Oggetto. */
	private static void assertRarityCaps(List<CardDefinition> deck) {
		Map<String, Long> byRarity = deck.stream()
				.collect(Collectors.groupingBy(CardDefinition::rarity, Collectors.counting()));
		assertThat(byRarity.getOrDefault("Legendary", 0L)).isLessThanOrEqualTo(2);
		assertThat(byRarity.getOrDefault("Mythic", 0L)).isLessThanOrEqualTo(2);
		assertThat(byRarity.getOrDefault("Mutant", 0L)).isLessThanOrEqualTo(1);
		assertThat(byRarity.getOrDefault("Rare", 0L)).isLessThanOrEqualTo(5);
	}
}
