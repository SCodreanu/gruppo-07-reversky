package it.downfall.reversky.api.pack.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.test.util.ReflectionTestUtils;

import it.downfall.reversky.api.catalog.entity.CardSubtype;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.pack.entity.Pack;

/**
 * Carte, rarità e pacchetti costruiti senza database per i test di {@link CardPool} e {@link PackDrawer}.
 */
final class PackFixtures {

	static final Rarity COMMON = rarity(1, "Common");
	static final Rarity UNCOMMON = rarity(2, "Uncommon");
	static final Rarity RARE = rarity(3, "Rare");
	static final Rarity MYTHIC = rarity(4, "Mythic");
	static final Rarity LEGENDARY = rarity(5, "Legendary");
	static final Rarity MUTANT = rarity(6, "Mutant");

	private static int nextId = 1;

	private PackFixtures() {
	}

	static CatalogCard creature(Rarity rarity) {
		return card(CardType.CREATURE, rarity, null);
	}

	static CatalogCard card(CardType type, Rarity rarity, CardSubtype subtype) {
		boolean creature = type == CardType.CREATURE;
		int id = nextId++;
		CatalogCard card = new CatalogCard(null, rarity, null, null, type + " " + id, type, subtype,
			creature ? null : "TEST_EFFECT", "/images/test.webp", null, null, creature ? 5 : null, creature ? 5 : null, null);
		ReflectionTestUtils.setField(card, "id", id);
		return card;
	}

	/**
	 * Pacchetto con gli slot e i bonus indicati (rarità e quantità o unità su 10.000).
	 */
	static Pack pack(Map<Rarity, Integer> slots, Map<Rarity, Integer> bonuses) {
		Pack pack = new Pack("Test pack", 100, null, "packs/test.webp");
		ReflectionTestUtils.setField(pack, "id", 1);
		pack.replaceSlots(new LinkedHashMap<>(slots));
		pack.replaceBonuses(new LinkedHashMap<>(bonuses));
		return pack;
	}

	private static Rarity rarity(int order, String name) {
		Rarity rarity = new Rarity(name, 3, null, order);
		ReflectionTestUtils.setField(rarity, "id", order);
		return rarity;
	}
}
