package it.downfall.reversky.api.pack.service;

import static it.downfall.reversky.api.pack.service.PackFixtures.COMMON;
import static it.downfall.reversky.api.pack.service.PackFixtures.RARE;
import static it.downfall.reversky.api.pack.service.PackFixtures.card;
import static it.downfall.reversky.api.pack.service.PackFixtures.creature;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.api.catalog.entity.CardSubtype;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.entity.Faction;
import it.downfall.reversky.api.catalog.entity.Tag;

/**
 * Filtro dei tag di {@link CardPool} (API-21, docs/pacchetti, "Filtro delle carte"), senza database.
 */
class CardPoolTagsTest {

	private static final Tag DREAM = new Tag("DREAM");
	private static final Tag EVENT = new Tag("CHRISTMAS_2026");

	@Test
	@DisplayName("A pack without tags: every active original card, never a variant, even one with tags")
	void noTags_activeOriginalsOnly() {
		// Arrange
		CatalogCard plain = creature(COMMON);
		CatalogCard original = creature(COMMON);
		CatalogCard variant = variantOf(original);
		variant.replaceTags(List.of(EVENT));

		// Act
		CardPool pool = new CardPool(List.of(plain, original, variant), Set.of());

		// Assert
		assertThat(pool.candidatesFor(COMMON)).containsExactlyInAnyOrder(plain, original);
	}

	@Test
	@DisplayName("A tagged pack: cards with the tag of their own or of their first or second faction; others never")
	void tags_ownOrFactionTags() {
		// Arrange
		Faction dream = faction(DREAM);
		CatalogCard own = creature(COMMON);
		own.replaceTags(List.of(DREAM));
		CatalogCard ofFirstFaction = withFactions(creature(COMMON), dream, null);
		CatalogCard ofSecondFaction = withFactions(creature(COMMON), faction(), dream);
		CatalogCard other = withFactions(creature(COMMON), faction(), null);

		// Act
		CardPool pool = new CardPool(List.of(own, ofFirstFaction, ofSecondFaction, other), Set.of("DREAM"));

		// Assert
		assertThat(pool.candidatesFor(COMMON)).containsExactlyInAnyOrder(own, ofFirstFaction, ofSecondFaction);
	}

	@Test
	@DisplayName("A variant drops from a tagged pack only with its own tag: its faction's tag is not enough")
	void variant_onlyWithOwnTags() {
		// Arrange: le due varianti sono della fazione DREAM, solo una ha anche il tag dell'evento
		Faction dream = faction(DREAM);
		CatalogCard original = withFactions(creature(COMMON), dream, null);
		CatalogCard eventVariant = withFactions(variantOf(original), dream, null);
		eventVariant.replaceTags(List.of(EVENT));
		CatalogCard plainVariant = withFactions(variantOf(original), dream, null);

		// Act
		CardPool eventPack = new CardPool(List.of(original, eventVariant, plainVariant), Set.of("CHRISTMAS_2026"));
		CardPool dreamPack = new CardPool(List.of(original, eventVariant, plainVariant), Set.of("DREAM"));

		// Assert
		assertThat(eventPack.candidatesFor(COMMON)).containsExactly(eventVariant);
		assertThat(dreamPack.candidatesFor(COMMON)).containsExactly(original);
	}

	@Test
	@DisplayName("SPELL and HIDDEN cards have no faction: in a tagged pack they drop only with their own tags")
	void spellAndHidden_onlyOwnTags() {
		// Arrange
		CatalogCard spell = card(CardType.SPELL, RARE, CardSubtype.DAMAGE);
		CatalogCard hidden = card(CardType.HIDDEN, RARE, CardSubtype.SURVIVAL);
		CatalogCard taggedSpell = card(CardType.SPELL, RARE, CardSubtype.SHIELD);
		taggedSpell.replaceTags(List.of(DREAM));

		// Act
		CardPool pool = new CardPool(List.of(spell, hidden, taggedSpell), Set.of("DREAM"));

		// Assert
		assertThat(pool.candidatesFor(RARE)).containsExactly(taggedSpell);
	}

	@Test
	@DisplayName("Deactivated cards never drop, even with the pack's tag")
	void inactive_neverDrops() {
		CatalogCard inactive = creature(COMMON);
		inactive.replaceTags(List.of(DREAM));
		inactive.deactivate();

		assertThat(new CardPool(List.of(inactive), Set.of("DREAM")).hasCards(COMMON)).isFalse();
	}

	private static CatalogCard variantOf(CatalogCard original) {
		return new CatalogCard(original, original.getRarity(), null, null, "Variant", CardType.CREATURE, null, null,
			"/images/v.webp", null, null, 5, 5, null);
	}

	private static CatalogCard withFactions(CatalogCard card, Faction faction, Faction secondary) {
		card.changeFactions(faction, secondary);
		return card;
	}

	private static Faction faction(Tag... tags) {
		Faction faction = new Faction("Faction", "factions/test.svg", null);
		faction.replaceTags(List.of(tags));
		return faction;
	}
}
