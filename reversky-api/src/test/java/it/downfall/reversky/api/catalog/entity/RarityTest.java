package it.downfall.reversky.api.catalog.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link Rarity}: statistiche massime (docs/backoffice, regola 5). Test unitario, senza Spring né database.
 */
class RarityTest {

	@Test
	@DisplayName("ATK above the maximum exceeds it, ATK equal to the maximum does not")
	void exceedsMaxAtk_aboveAndEqual() {
		// Arrange
		Rarity rarity = rarity(10, 12);

		// Act + Assert
		assertThat(rarity.exceedsMaxAtk(11)).isTrue();
		assertThat(rarity.exceedsMaxAtk(10)).isFalse();
		assertThat(rarity.exceedsMaxAtk(0)).isFalse();
	}

	@Test
	@DisplayName("DEF above the maximum exceeds it, DEF equal to the maximum does not")
	void exceedsMaxDef_aboveAndEqual() {
		// Arrange
		Rarity rarity = rarity(10, 12);

		// Act + Assert
		assertThat(rarity.exceedsMaxDef(13)).isTrue();
		assertThat(rarity.exceedsMaxDef(12)).isFalse();
		assertThat(rarity.exceedsMaxDef(1)).isFalse();
	}

	@Test
	@DisplayName("A rarity without maximums never rejects a stat")
	void exceeds_noMaximum_returnsFalse() {
		// Arrange: come la Mutant, massimi non ancora definiti
		Rarity rarity = rarity(null, null);

		// Act + Assert
		assertThat(rarity.exceedsMaxAtk(1_000)).isFalse();
		assertThat(rarity.exceedsMaxDef(1_000)).isFalse();
	}

	@Test
	@DisplayName("A card without stats never exceeds the maximums")
	void exceeds_nullStat_returnsFalse() {
		// Arrange
		Rarity rarity = rarity(10, 12);

		// Act + Assert: Spell, Hidden e Terreni hanno ATK e DEF null
		assertThat(rarity.exceedsMaxAtk(null)).isFalse();
		assertThat(rarity.exceedsMaxDef(null)).isFalse();
	}

	@Test
	@DisplayName("ATK and DEF are checked against their own maximum")
	void exceeds_separateMaximums() {
		// Arrange: massimo ATK nullo, massimo DEF 5
		Rarity rarity = rarity(null, 5);

		// Act + Assert
		assertThat(rarity.exceedsMaxAtk(50)).isFalse();
		assertThat(rarity.exceedsMaxDef(6)).isTrue();
	}

	private static Rarity rarity(Integer maxAtk, Integer maxDef) {
		return new Rarity("Test", 3, null, maxAtk, maxDef, 1_000);
	}
}
