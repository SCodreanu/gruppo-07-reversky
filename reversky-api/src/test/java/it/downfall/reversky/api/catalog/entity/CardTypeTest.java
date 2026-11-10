package it.downfall.reversky.api.catalog.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link CardType}: test unitario, senza Spring né database.
 * <p>
 * Le regole sono le stesse dei vincoli di V3 (chk_catalog_cards_subtype_matches_type,
 * chk_catalog_cards_stats_match_type, chk_catalog_cards_effect_required, chk_catalog_cards_faction_matches_type): il service le usa
 * per rispondere 422 prima che il database rifiuti la riga. La regola sulle rarità invece non ha
 * un vincolo nel database e la controlla solo il service.
 */
class CardTypeTest {

	// --- acceptsSubtype ---

	@Test
	@DisplayName("Creatures, terrains and items accept no subtype")
	void acceptsSubtype_typeWithoutSubtypes_acceptsOnlyNull() {
		// Act + Assert
		assertThat(CardType.CREATURE.acceptsSubtype(null)).isTrue();
		assertThat(CardType.TERRAIN.acceptsSubtype(null)).isTrue();
		assertThat(CardType.ITEM.acceptsSubtype(null)).isTrue();

		for (CardSubtype subtype : CardSubtype.values()) {
			assertThat(CardType.CREATURE.acceptsSubtype(subtype)).as("CREATURE with %s", subtype).isFalse();
			assertThat(CardType.TERRAIN.acceptsSubtype(subtype)).as("TERRAIN with %s", subtype).isFalse();
			assertThat(CardType.ITEM.acceptsSubtype(subtype)).as("ITEM with %s", subtype).isFalse();
		}
	}

	@Test
	@DisplayName("A spell or a hidden card without a subtype is rejected")
	void acceptsSubtype_nullForTypeWithSubtypes_returnsFalse() {
		// Act + Assert: Set.of(...).contains(null) lancia NullPointerException, il null va gestito prima
		assertThat(CardType.SPELL.acceptsSubtype(null)).isFalse();
		assertThat(CardType.HIDDEN.acceptsSubtype(null)).isFalse();
	}

	@Test
	@DisplayName("A spell accepts only DAMAGE, SHIELD and BURN")
	void acceptsSubtype_spell_acceptsOnlyItsSubtypes() {
		// Act + Assert
		assertThat(CardType.SPELL.acceptsSubtype(CardSubtype.DAMAGE)).isTrue();
		assertThat(CardType.SPELL.acceptsSubtype(CardSubtype.SHIELD)).isTrue();
		assertThat(CardType.SPELL.acceptsSubtype(CardSubtype.BURN)).isTrue();
		assertThat(CardType.SPELL.acceptsSubtype(CardSubtype.SURVIVAL)).isFalse();
	}

	@Test
	@DisplayName("A hidden card accepts only SURVIVAL and DAMAGE")
	void acceptsSubtype_hidden_acceptsOnlyItsSubtypes() {
		// Act + Assert
		assertThat(CardType.HIDDEN.acceptsSubtype(CardSubtype.SURVIVAL)).isTrue();
		assertThat(CardType.HIDDEN.acceptsSubtype(CardSubtype.DAMAGE)).isTrue();
		assertThat(CardType.HIDDEN.acceptsSubtype(CardSubtype.SHIELD)).isFalse();
		assertThat(CardType.HIDDEN.acceptsSubtype(CardSubtype.BURN)).isFalse();
	}

	// --- hasStats e requiresEffect ---

	@Test
	@DisplayName("Only creatures have stats")
	void hasStats_onlyCreature_returnsTrue() {
		// Act + Assert
		assertThat(CardType.CREATURE.hasStats()).isTrue();
		assertThat(CardType.TERRAIN.hasStats()).isFalse();
		assertThat(CardType.SPELL.hasStats()).isFalse();
		assertThat(CardType.HIDDEN.hasStats()).isFalse();
		assertThat(CardType.ITEM.hasStats()).isFalse();
	}

	@Test
	@DisplayName("Every type except creatures requires an effect")
	void requiresEffect_allButCreature_returnsTrue() {
		// Act + Assert
		assertThat(CardType.CREATURE.requiresEffect()).isFalse();
		assertThat(CardType.TERRAIN.requiresEffect()).isTrue();
		assertThat(CardType.SPELL.requiresEffect()).isTrue();
		assertThat(CardType.HIDDEN.requiresEffect()).isTrue();
		assertThat(CardType.ITEM.requiresEffect()).isTrue();
	}

	// --- hasFaction ---

	@Test
	@DisplayName("Creatures, terrains and items have a faction; spells and hidden cards are shared")
	void hasFaction_allButSpellAndHidden_returnsTrue() {
		// Act + Assert
		assertThat(CardType.CREATURE.hasFaction()).isTrue();
		assertThat(CardType.TERRAIN.hasFaction()).isTrue();
		assertThat(CardType.ITEM.hasFaction()).isTrue();
		assertThat(CardType.SPELL.hasFaction()).isFalse();
		assertThat(CardType.HIDDEN.hasFaction()).isFalse();
	}

	// --- acceptsRarity (docs/backoffice, regola 4) ---

	@Test
	@DisplayName("A creature accepts any rarity")
	void acceptsRarity_creature_acceptsAnyOrder() {
		// Act + Assert
		for (int order : new int[] { 1, 2, 3, 4, 5, 6, 1_000 }) {
			assertThat(CardType.CREATURE.acceptsRarity(rarity("Any", order))).as("order %d", order).isTrue();
		}
	}

	@Test
	@DisplayName("Spells and hidden cards accept only Common and Uncommon")
	void acceptsRarity_spellAndHidden_acceptOnlyCommonAndUncommon() {
		// Act + Assert
		for (CardType type : new CardType[] { CardType.SPELL, CardType.HIDDEN }) {
			assertThat(type.acceptsRarity(rarity("Common", Rarity.COMMON_ORDER))).as("%s Common", type).isTrue();
			assertThat(type.acceptsRarity(rarity("Uncommon", Rarity.UNCOMMON_ORDER))).as("%s Uncommon", type).isTrue();
			assertThat(type.acceptsRarity(rarity("Rare", Rarity.RARE_ORDER))).as("%s Rare", type).isFalse();
			assertThat(type.acceptsRarity(rarity("Mythic", 4))).as("%s Mythic", type).isFalse();
		}
	}

	@Test
	@DisplayName("Terrains accept only Rare")
	void acceptsRarity_terrain_acceptsOnlyRare() {
		// Act + Assert
		assertThat(CardType.TERRAIN.acceptsRarity(rarity("Rare", Rarity.RARE_ORDER))).isTrue();
		assertThat(CardType.TERRAIN.acceptsRarity(rarity("Common", Rarity.COMMON_ORDER))).isFalse();
		assertThat(CardType.TERRAIN.acceptsRarity(rarity("Uncommon", Rarity.UNCOMMON_ORDER))).isFalse();
		assertThat(CardType.TERRAIN.acceptsRarity(rarity("Mythic", 4))).isFalse();
	}

	@Test
	@DisplayName("Items accept only Rare")
	void acceptsRarity_item_acceptsOnlyRare() {
		// Act + Assert
		assertThat(CardType.ITEM.acceptsRarity(rarity("Rare", Rarity.RARE_ORDER))).isTrue();
		assertThat(CardType.ITEM.acceptsRarity(rarity("Common", Rarity.COMMON_ORDER))).isFalse();
		assertThat(CardType.ITEM.acceptsRarity(rarity("Mythic", 4))).isFalse();
	}

	@Test
	@DisplayName("The rarity is recognised by its order, not by its name")
	void acceptsRarity_renamedRarity_usesSortingOrder() {
		// Act + Assert: una Rare rinominata resta ammessa, un nome "Rare" con un altro ordine no
		assertThat(CardType.TERRAIN.acceptsRarity(rarity("Renamed rare", Rarity.RARE_ORDER))).isTrue();
		assertThat(CardType.TERRAIN.acceptsRarity(rarity("Rare", 4))).isFalse();
	}

	// Rarità senza massimi, costruita senza database
	private static Rarity rarity(String name, int sortingOrder) {
		return new Rarity(name, 3, null, sortingOrder);
	}
}
