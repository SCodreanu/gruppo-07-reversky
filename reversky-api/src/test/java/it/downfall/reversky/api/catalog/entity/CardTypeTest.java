package it.downfall.reversky.api.catalog.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link CardType}: test unitario, senza Spring né database.
 * <p>
 * Le regole sono le stesse dei vincoli di V5 (chk_catalog_cards_subtype_matches_type,
 * chk_catalog_cards_stats_match_type, chk_catalog_cards_effect_required): il service le usa
 * per rispondere 422 prima che il database rifiuti la riga.
 */
class CardTypeTest {

	// --- acceptsSubtype ---

	@Test
	@DisplayName("Creatures and terrains accept no subtype")
	void acceptsSubtype_typeWithoutSubtypes_acceptsOnlyNull() {
		// Act + Assert
		assertThat(CardType.CREATURE.acceptsSubtype(null)).isTrue();
		assertThat(CardType.TERRAIN.acceptsSubtype(null)).isTrue();

		for (CardSubtype subtype : CardSubtype.values()) {
			assertThat(CardType.CREATURE.acceptsSubtype(subtype)).as("CREATURE with %s", subtype).isFalse();
			assertThat(CardType.TERRAIN.acceptsSubtype(subtype)).as("TERRAIN with %s", subtype).isFalse();
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
	}

	@Test
	@DisplayName("Every type except creatures requires an effect")
	void requiresEffect_allButCreature_returnsTrue() {
		// Act + Assert
		assertThat(CardType.CREATURE.requiresEffect()).isFalse();
		assertThat(CardType.TERRAIN.requiresEffect()).isTrue();
		assertThat(CardType.SPELL.requiresEffect()).isTrue();
		assertThat(CardType.HIDDEN.requiresEffect()).isTrue();
	}
}
