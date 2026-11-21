package it.downfall.reversky.api.pack.service;

import static it.downfall.reversky.api.pack.service.PackFixtures.COMMON;
import static it.downfall.reversky.api.pack.service.PackFixtures.LEGENDARY;
import static it.downfall.reversky.api.pack.service.PackFixtures.MYTHIC;
import static it.downfall.reversky.api.pack.service.PackFixtures.RARE;
import static it.downfall.reversky.api.pack.service.PackFixtures.UNCOMMON;
import static it.downfall.reversky.api.pack.service.PackFixtures.card;
import static it.downfall.reversky.api.pack.service.PackFixtures.creature;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.api.catalog.entity.CardSubtype;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;

/**
 * {@link CardPool} da solo (docs/pacchetti, "Rarità senza carte").
 */
class CardPoolTest {

	@Test
	@DisplayName("A slot draws from the cards of its own rarity")
	void candidatesFor_rarityWithCards_returnsThem() {
		// Arrange
		CatalogCard common = creature(COMMON);
		CatalogCard rare = creature(RARE);
		CardPool pool = new CardPool(List.of(common, rare));

		// Act + Assert
		assertThat(pool.candidatesFor(COMMON)).containsExactly(common);
		assertThat(pool.candidatesFor(RARE)).containsExactly(rare);
	}

	@Test
	@DisplayName("A slot without cards falls back to the nearest lower rarity first")
	void candidatesFor_noCards_fallsBackDown() {
		// Arrange: niente Rare, ma Uncommon e Common
		CatalogCard common = creature(COMMON);
		CatalogCard uncommon = creature(UNCOMMON);
		CardPool pool = new CardPool(List.of(common, uncommon, creature(LEGENDARY)));

		// Act + Assert
		assertThat(pool.candidatesFor(RARE)).containsExactly(uncommon);
	}

	@Test
	@DisplayName("With nothing below, the slot falls back to the nearest higher rarity")
	void candidatesFor_nothingBelow_fallsBackUp() {
		// Arrange
		CatalogCard rare = creature(RARE);
		CardPool pool = new CardPool(List.of(rare, creature(LEGENDARY)));

		// Act + Assert
		assertThat(pool.candidatesFor(COMMON)).containsExactly(rare);
	}

	@Test
	@DisplayName("The fallback never picks a Terrain or an Item, but a Rare slot does")
	void candidatesFor_fallback_skipsTerrainsAndItems() {
		// Arrange: le sole Rare sono un Terreno, un Oggetto e una Spell
		CatalogCard terrain = card(CardType.TERRAIN, RARE, null);
		CatalogCard item = card(CardType.ITEM, RARE, null);
		CatalogCard spell = card(CardType.SPELL, RARE, CardSubtype.BURN);
		CatalogCard mythic = creature(MYTHIC);
		CardPool pool = new CardPool(List.of(terrain, item, spell, mythic));

		// Act + Assert
		assertThat(pool.candidatesFor(COMMON)).containsExactly(spell);
		assertThat(pool.candidatesFor(RARE)).containsExactlyInAnyOrder(terrain, item, spell);
	}

	@Test
	@DisplayName("Only Terrains and Items available: a Common slot finds nothing")
	void candidatesFor_onlyTerrains_returnsEmpty() {
		// Arrange
		CardPool pool = new CardPool(List.of(card(CardType.TERRAIN, RARE, null), card(CardType.ITEM, RARE, null)));

		// Act + Assert
		assertThat(pool.candidatesFor(COMMON)).isEmpty();
	}

	@Test
	@DisplayName("Deactivated cards and variants never come out")
	void pool_skipsInactiveAndVariants() {
		// Arrange
		CatalogCard inactive = creature(COMMON);
		inactive.deactivate();
		CatalogCard original = creature(UNCOMMON);
		CatalogCard variant = new CatalogCard(original, UNCOMMON, null, null, "Variant", CardType.CREATURE, null, null,
			"/images/v.webp", null, null, 5, 5, null);
		CardPool pool = new CardPool(List.of(inactive, original, variant));

		// Act + Assert
		assertThat(pool.hasCards(COMMON)).isFalse();
		assertThat(pool.candidatesFor(UNCOMMON)).containsExactly(original);
	}
}
