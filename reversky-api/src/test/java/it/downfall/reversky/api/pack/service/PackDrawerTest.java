package it.downfall.reversky.api.pack.service;

import static it.downfall.reversky.api.pack.service.PackFixtures.COMMON;
import static it.downfall.reversky.api.pack.service.PackFixtures.LEGENDARY;
import static it.downfall.reversky.api.pack.service.PackFixtures.MUTANT;
import static it.downfall.reversky.api.pack.service.PackFixtures.MYTHIC;
import static it.downfall.reversky.api.pack.service.PackFixtures.RARE;
import static it.downfall.reversky.api.pack.service.PackFixtures.UNCOMMON;
import static it.downfall.reversky.api.pack.service.PackFixtures.card;
import static it.downfall.reversky.api.pack.service.PackFixtures.creature;
import static it.downfall.reversky.api.pack.service.PackFixtures.pack;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.pack.entity.Pack;
import it.downfall.reversky.api.pack.exception.PackSlotEmptyException;

/**
 * {@link PackDrawer} da solo, con generatori a seed fisso o costruiti a mano (docs/pacchetti, "Test minimi").
 */
class PackDrawerTest {

	// 3 Common, 2 Uncommon, 2 Rare: 7 carte
	private static final Map<Rarity, Integer> BASE_SLOTS = Map.of(COMMON, 3, UNCOMMON, 2, RARE, 2);
	private static final Map<Rarity, Integer> BASE_BONUSES = Map.of(MYTHIC, 1_000, LEGENDARY, 500, MUTANT, 250);

	private final CardPool fullPool = new CardPool(List.of(creature(COMMON), creature(COMMON), creature(UNCOMMON),
		creature(RARE), card(CardType.TERRAIN, RARE, null), creature(MYTHIC), creature(LEGENDARY), creature(MUTANT)));

	@Test
	@DisplayName("Without bonuses the cards follow the base slots, from the most common")
	void draw_noBonus_followsBaseSlots() {
		// Arrange: il generatore non fa mai uscire un bonus
		PackDrawer drawer = new PackDrawer(always(9_999));

		// Act
		List<CatalogCard> cards = drawer.draw(pack(BASE_SLOTS, BASE_BONUSES), fullPool);

		// Assert
		assertThat(rarities(cards)).containsExactly(COMMON, COMMON, COMMON, UNCOMMON, UNCOMMON, RARE, RARE);
	}

	@Test
	@DisplayName("Successful bonuses replace slots from the rarest, rarest bonus last, and the count never changes")
	void draw_allBonuses_replaceRarestSlots() {
		// Arrange: ogni tiro vale 0, quindi ogni bonus esce
		PackDrawer drawer = new PackDrawer(always(0));

		// Act
		List<CatalogCard> cards = drawer.draw(pack(BASE_SLOTS, BASE_BONUSES), fullPool);

		// Assert
		assertThat(cards).hasSize(7);
		assertThat(rarities(cards)).containsExactly(COMMON, COMMON, COMMON, UNCOMMON, MYTHIC, LEGENDARY, MUTANT);
	}

	@Test
	@DisplayName("With more successful bonuses than slots the rarest win")
	void draw_moreBonusesThanSlots_rarestWin() {
		// Arrange: un solo slot e tre bonus riusciti
		PackDrawer drawer = new PackDrawer(always(0));

		// Act
		List<CatalogCard> cards = drawer.draw(pack(Map.of(COMMON, 1), BASE_BONUSES), fullPool);

		// Assert
		assertThat(rarities(cards)).containsExactly(MUTANT);
	}

	@Test
	@DisplayName("A bonus of a rarity without cards never fires: the slot keeps its base rarity")
	void draw_bonusWithoutCards_doesNotFire() {
		// Arrange: nessuna Mutant nel catalogo
		CardPool pool = new CardPool(List.of(creature(COMMON), creature(RARE)));
		PackDrawer drawer = new PackDrawer(always(0));

		// Act
		List<CatalogCard> cards = drawer.draw(pack(Map.of(COMMON, 1, RARE, 1), Map.of(MUTANT, 10_000)), pool);

		// Assert
		assertThat(rarities(cards)).containsExactly(COMMON, RARE);
	}

	@Test
	@DisplayName("A Terrain never comes out of a Common slot, even with the fallback")
	void draw_commonSlotWithFallback_neverTerrain() {
		// Arrange: le uniche carte sono un Terreno Rare e una creatura Mythic
		CatalogCard mythic = creature(MYTHIC);
		CardPool pool = new CardPool(List.of(card(CardType.TERRAIN, RARE, null), mythic));
		PackDrawer drawer = new PackDrawer(new SplittableRandom(7));

		// Act
		List<CatalogCard> cards = drawer.draw(pack(Map.of(COMMON, 5), Map.of()), pool);

		// Assert
		assertThat(cards).hasSize(5).containsOnly(mythic);
	}

	@Test
	@DisplayName("A slot with no card at all stops the opening")
	void draw_noCardForSlot_throws() {
		// Arrange
		CardPool pool = new CardPool(List.of(card(CardType.ITEM, RARE, null)));
		PackDrawer drawer = new PackDrawer(new SplittableRandom(1));

		// Act + Assert
		assertThatThrownBy(() -> drawer.draw(pack(Map.of(COMMON, 1), Map.of()), pool))
			.isInstanceOf(PackSlotEmptyException.class);
	}

	@Test
	@DisplayName("Over 100,000 openings the bonus frequencies approach the configured odds")
	void draw_manyOpenings_frequenciesMatchOdds() {
		// Arrange: seed fisso, quindi il test è ripetibile
		PackDrawer drawer = new PackDrawer(new SplittableRandom(20261121L));
		Pack pack = pack(BASE_SLOTS, BASE_BONUSES);
		int openings = 100_000;
		int mythic = 0;
		int legendary = 0;
		int mutant = 0;

		// Act
		for (int i = 0; i < openings; i++) {
			List<Rarity> rarities = rarities(drawer.draw(pack, fullPool));
			mythic += rarities.contains(MYTHIC) ? 1 : 0;
			legendary += rarities.contains(LEGENDARY) ? 1 : 0;
			mutant += rarities.contains(MUTANT) ? 1 : 0;
		}

		// Assert: 10%, 5% e 2,5% con mezzo punto di tolleranza (lo scarto atteso è circa 0,1)
		assertThat(mythic * 100.0 / openings).isCloseTo(10.0, within(0.5));
		assertThat(legendary * 100.0 / openings).isCloseTo(5.0, within(0.5));
		assertThat(mutant * 100.0 / openings).isCloseTo(2.5, within(0.5));
	}

	// Generatore che restituisce sempre lo stesso valore (limitato al bound): 0 fa uscire ogni bonus
	private static RandomGenerator always(int value) {
		return new RandomGenerator() {
			@Override
			public long nextLong() {
				return value;
			}

			@Override
			public int nextInt(int bound) {
				return Math.min(value, bound - 1);
			}
		};
	}

	private static List<Rarity> rarities(List<CatalogCard> cards) {
		List<Rarity> rarities = new ArrayList<>();
		cards.forEach(card -> rarities.add(card.getRarity()));
		return rarities;
	}
}
