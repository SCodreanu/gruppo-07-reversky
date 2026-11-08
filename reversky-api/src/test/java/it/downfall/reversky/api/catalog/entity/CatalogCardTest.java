package it.downfall.reversky.api.catalog.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link CatalogCard}: test unitario, senza Spring né database.
 * <p>
 * Le regole (chi può diventare variante, quali valori sono validi) le controlla il service:
 * qui si verifica solo che i metodi dell'entità cambino i campi giusti.
 */
class CatalogCardTest {

	// Rarità costruite senza database: qui servono solo come oggetti da confrontare
	private static final Rarity COMMON = new Rarity("Common", 3, null, null, null, Rarity.COMMON_ORDER);
	private static final Rarity UNCOMMON = new Rarity("Uncommon", 3, null, null, null, Rarity.UNCOMMON_ORDER);

	// --- costruttore e isVariant ---

	@Test
	@DisplayName("A new card is active")
	void constructor_newCard_isActive() {
		// Act
		CatalogCard card = creature(null);

		// Assert
		assertThat(card.isActive()).isTrue();
	}

	@Test
	@DisplayName("A card without variantOf is an original")
	void isVariant_withoutVariantOf_returnsFalse() {
		// Act
		CatalogCard card = creature(null);

		// Assert
		assertThat(card.isVariant()).isFalse();
	}

	@Test
	@DisplayName("A card with variantOf is a variant")
	void isVariant_withVariantOf_returnsTrue() {
		// Arrange
		CatalogCard original = creature(null);

		// Act
		CatalogCard variant = creature(original);

		// Assert
		assertThat(variant.isVariant()).isTrue();
	}

	// --- copyRulesFrom ---

	@Test
	@DisplayName("Copying the rules takes every game field from the original")
	void copyRulesFrom_original_copiesAllGameFields() {
		// Arrange: originale Spell Uncommon, variante ancora creatura Common con valori diversi
		CatalogCard original = new CatalogCard(null, UNCOMMON, "Original", CardType.SPELL, CardSubtype.BURN, "BURN_TWO",
				"/images/original.webp", "Burns for 2", "Original flavor", null, null, 2);
		CatalogCard variant = creature(original);

		// Act
		variant.copyRulesFrom(original);

		// Assert
		assertThat(variant.getRarity()).isSameAs(UNCOMMON);
		assertThat(variant.getCardType()).isEqualTo(CardType.SPELL);
		assertThat(variant.getCardSubtype()).isEqualTo(CardSubtype.BURN);
		assertThat(variant.getEffectCode()).isEqualTo("BURN_TWO");
		assertThat(variant.getEffectDescription()).isEqualTo("Burns for 2");
		assertThat(variant.getAtk()).isNull();
		assertThat(variant.getDef()).isNull();
		assertThat(variant.getCopyLimit()).isEqualTo(2);
	}

	@Test
	@DisplayName("Copying the rules keeps name, image and flavor text of the variant")
	void copyRulesFrom_original_keepsOwnDetails() {
		// Arrange
		CatalogCard original = creature(null);
		CatalogCard variant = new CatalogCard(original, COMMON, "Event name", CardType.CREATURE, null, null,
				"/images/event.webp", null, "Event flavor", 1, 1, null);

		// Act
		variant.copyRulesFrom(original);

		// Assert
		assertThat(variant.getName()).isEqualTo("Event name");
		assertThat(variant.getImagePath()).isEqualTo("/images/event.webp");
		assertThat(variant.getFlavorText()).isEqualTo("Event flavor");
	}

	// --- updateRules ---

	@Test
	@DisplayName("Updating the rules changes the rarity too")
	void updateRules_newRarity_changesRarity() {
		// Arrange
		CatalogCard card = creature(null);

		// Act
		card.updateRules(UNCOMMON, CardType.CREATURE, null, null, null, 7, 8, null);

		// Assert
		assertThat(card.getRarity()).isSameAs(UNCOMMON);
		assertThat(card.getAtk()).isEqualTo(7);
	}

	// --- updateDetails e changeVariantOf ---

	@Test
	@DisplayName("Updating the details changes only name and flavor text")
	void updateDetails_newValues_changesNameAndFlavorText() {
		// Arrange
		CatalogCard card = creature(null);

		// Act
		card.updateDetails("New name", "New flavor");

		// Assert
		assertThat(card.getName()).isEqualTo("New name");
		assertThat(card.getFlavorText()).isEqualTo("New flavor");
		assertThat(card.getAtk()).isEqualTo(5);
	}

	@Test
	@DisplayName("Changing variantOf to null makes the card an original again")
	void changeVariantOf_null_makesOriginal() {
		// Arrange
		CatalogCard variant = creature(creature(null));

		// Act
		variant.changeVariantOf(null);

		// Assert
		assertThat(variant.isVariant()).isFalse();
		assertThat(variant.getVariantOf()).isNull();
	}

	// --- activate e deactivate ---

	@Test
	@DisplayName("Deactivating and activating switch the active flag")
	void deactivateAndActivate_switchActiveFlag() {
		// Arrange
		CatalogCard card = creature(null);

		// Act + Assert
		card.deactivate();
		assertThat(card.isActive()).isFalse();

		card.activate();
		assertThat(card.isActive()).isTrue();
	}

	// Creatura Common valida con ATK 5 e DEF 6; variantOf null = originale
	private CatalogCard creature(CatalogCard variantOf) {
		return new CatalogCard(variantOf, COMMON, "Creature", CardType.CREATURE, null, null,
				"/images/creature.webp", null, "Flavor", 5, 6, null);
	}
}
