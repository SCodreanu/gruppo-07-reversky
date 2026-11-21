package it.downfall.reversky.api.deck.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import it.downfall.reversky.api.catalog.entity.CardSubtype;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.deck.dto.DeckViolation;

/**
 * Regole del mazzo (docs/mazzi, "Come si valida"), senza accesso al database: riceve le carte con rarità e carta
 * originale già caricate e restituisce <b>tutte</b> le violazioni, non solo la prima, così il frontend le mostra
 * insieme.
 * <p>
 * Ordine delle violazioni: carte totali, composizione per tipo, Hidden per sottotipo, copie, bilanciamento per
 * rarità, possesso, carte disattivate. Dentro ogni controllo le carte sono in ordine di id e le rarità di {@code sorting_order}.
 */
@Component
public class DeckValidator {

	public static final String SIZE = "DECK_SIZE";
	public static final String CREATURE_COUNT = "DECK_CREATURE_COUNT";
	public static final String SPELL_COUNT = "DECK_SPELL_COUNT";
	public static final String HIDDEN_COUNT = "DECK_HIDDEN_COUNT";
	public static final String HIDDEN_SUBTYPE_LIMIT = "DECK_HIDDEN_SUBTYPE_LIMIT";
	public static final String ITEM_COUNT = "DECK_ITEM_COUNT";
	public static final String TERRAIN_COUNT = "DECK_TERRAIN_COUNT";
	public static final String COPY_LIMIT = "DECK_COPY_LIMIT";
	public static final String BALANCE_LIMIT = "DECK_BALANCE_LIMIT";
	public static final String NOT_OWNED = "DECK_NOT_OWNED";
	public static final String INACTIVE_CARD = "DECK_INACTIVE_CARD";

	/**
	 * Controlla il mazzo con le regole indicate.
	 *
	 * @param rules             composizione dal rule set attivo
	 * @param cards             copie per carta; ogni carta ha rarità e, se è una variante, la carta originale con la sua
	 *                          rarità
	 * @param availableByCardId copie disponibili dell'utente per id di carta (possedute meno riservate); una carta che
	 *                          manca vale 0
	 * @return le violazioni trovate, lista vuota se il mazzo è valido
	 */
	public List<DeckViolation> validate(DeckRules rules, Map<CatalogCard, Integer> cards,
			Map<Integer, Integer> availableByCardId) {
		Map<CatalogCard, Integer> sorted = sortById(cards);
		List<DeckViolation> violations = new ArrayList<>();

		checkSize(rules, sorted, violations);
		checkComposition(rules, sorted, violations);
		checkHiddenSubtypes(rules, sorted, violations);
		checkCopyLimits(sorted, violations);
		checkBalanceLimits(sorted, violations);
		checkOwnership(sorted, availableByCardId, violations);
		checkActiveCards(sorted, violations);

		return violations;
	}

	private static void checkSize(DeckRules rules, Map<CatalogCard, Integer> cards, List<DeckViolation> violations) {
		int total = cards.values().stream().mapToInt(Integer::intValue).sum();
		if (total != rules.deckSize()) {
			violations.add(new DeckViolation(SIZE,
				"The deck has " + total + " cards, " + rules.deckSize() + " are required", null));
		}
	}

	// Numero esatto per ogni tipo (docs/mazzi): né uno in meno né uno in più
	private static void checkComposition(DeckRules rules, Map<CatalogCard, Integer> cards, List<DeckViolation> violations) {
		checkTypeCount(cards, CardType.CREATURE, rules.creatures(), CREATURE_COUNT, "creature", "creatures", violations);
		checkTypeCount(cards, CardType.SPELL, rules.spells(), SPELL_COUNT, "Spell", "Spells", violations);
		checkTypeCount(cards, CardType.HIDDEN, rules.hidden(), HIDDEN_COUNT, "Hidden card", "Hidden cards", violations);
		checkTypeCount(cards, CardType.ITEM, rules.items(), ITEM_COUNT, "Item", "Items", violations);
		checkTypeCount(cards, CardType.TERRAIN, rules.terrains(), TERRAIN_COUNT, "Terrain", "Terrains", violations);
	}

	// Il messaggio lo legge il giocatore: singolare o plurale secondo il numero richiesto
	private static void checkTypeCount(Map<CatalogCard, Integer> cards, CardType type, int required, String code,
			String singular, String plural, List<DeckViolation> violations) {
		int count = countWhere(cards, card -> card.getCardType() == type);
		if (count != required) {
			String label = required == 1 ? singular : plural;
			violations.add(new DeckViolation(code,
				"The deck needs exactly " + required + " " + label + ", it has " + count, null));
		}
	}

	private static void checkHiddenSubtypes(DeckRules rules, Map<CatalogCard, Integer> cards, List<DeckViolation> violations) {
		checkHiddenSubtype(cards, CardSubtype.SURVIVAL, rules.maxHiddenSurvival(), violations);
		checkHiddenSubtype(cards, CardSubtype.DAMAGE, rules.maxHiddenDamage(), violations);
	}

	private static void checkHiddenSubtype(Map<CatalogCard, Integer> cards, CardSubtype subtype, int max,
			List<DeckViolation> violations) {
		int count = countWhere(cards, card -> card.getCardType() == CardType.HIDDEN && card.getCardSubtype() == subtype);
		if (count > max) {
			violations.add(new DeckViolation(HIDDEN_SUBTYPE_LIMIT,
				"Too many " + subtype + " Hidden cards (" + count + "/" + max + ")", null));
		}
	}

	// Una variante conta insieme alla sua carta originale, con il limite dell'originale: min(deck_limit, copy_limit)
	private static void checkCopyLimits(Map<CatalogCard, Integer> cards, List<DeckViolation> violations) {
		Map<Integer, Integer> copiesByOriginal = new TreeMap<>();
		Map<Integer, CatalogCard> originals = new LinkedHashMap<>();
		cards.forEach((card, quantity) -> {
			CatalogCard original = card.isVariant() ? card.getVariantOf() : card;
			originals.putIfAbsent(original.getId(), original);
			copiesByOriginal.merge(original.getId(), quantity, Integer::sum);
		});

		copiesByOriginal.forEach((originalId, copies) -> {
			CatalogCard original = originals.get(originalId);
			int limit = copyLimit(original);
			if (copies > limit) {
				violations.add(new DeckViolation(COPY_LIMIT,
					"Too many copies of '" + original.getName() + "' (" + copies + "/" + limit + ")", originalId));
			}
		});
	}

	private static int copyLimit(CatalogCard original) {
		int rarityLimit = original.getRarity().getDeckLimit();
		Integer cardLimit = original.getCopyLimit();
		return cardLimit == null ? rarityLimit : Math.min(rarityLimit, cardLimit);
	}

	// Massimo per rarità (max_per_deck, vuoto = nessun limite): Terreno e Oggetto sono Rare e ne occupano un posto
	private static void checkBalanceLimits(Map<CatalogCard, Integer> cards, List<DeckViolation> violations) {
		Map<Rarity, Integer> copiesByRarity = new TreeMap<>(Comparator.comparing(Rarity::getSortingOrder));
		cards.forEach((card, quantity) -> copiesByRarity.merge(card.getRarity(), quantity, Integer::sum));

		copiesByRarity.forEach((rarity, copies) -> {
			Integer max = rarity.getMaxPerDeck();
			if (max != null && copies > max) {
				violations.add(new DeckViolation(BALANCE_LIMIT,
					"Too many " + rarity.getName() + " cards (" + copies + "/" + max + ")", null));
			}
		});
	}

	// Per ogni carta servono abbastanza copie disponibili: le varianti sono carte a sé nella collezione
	private static void checkOwnership(Map<CatalogCard, Integer> cards, Map<Integer, Integer> availableByCardId,
			List<DeckViolation> violations) {
		cards.forEach((card, quantity) -> {
			int available = availableByCardId.getOrDefault(card.getId(), 0);
			if (quantity > available) {
				String copies = available == 1 ? "copy" : "copies";
				violations.add(new DeckViolation(NOT_OWNED, "You have " + available + " available " + copies + " of '"
					+ card.getName() + "', the deck contains " + quantity, card.getId()));
			}
		});
	}

	// Una carta disattivata dal backoffice non si può usare finché non la si toglie (Q-07)
	private static void checkActiveCards(Map<CatalogCard, Integer> cards, List<DeckViolation> violations) {
		cards.keySet().stream()
			.filter(card -> !card.isActive())
			.forEach(card -> violations.add(new DeckViolation(INACTIVE_CARD,
				"'" + card.getName() + "' is no longer available and must be removed from the deck", card.getId())));
	}

	private static int countWhere(Map<CatalogCard, Integer> cards, Predicate<CatalogCard> condition) {
		return cards.entrySet().stream()
			.filter(entry -> condition.test(entry.getKey()))
			.mapToInt(Map.Entry::getValue)
			.sum();
	}

	private static Map<CatalogCard, Integer> sortById(Map<CatalogCard, Integer> cards) {
		Map<CatalogCard, Integer> sorted = new LinkedHashMap<>();
		cards.entrySet().stream()
			.sorted(Map.Entry.comparingByKey(Comparator.comparing(CatalogCard::getId)))
			.forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
		return sorted;
	}
}
