package it.downfall.reversky.engine.balance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CardType;

/**
 * Costruisce mazzi casuali di una fazione dal catalogo, nei limiti del regolamento dell'API (reversky-api/docs/mazzi),
 * deciso il 20 novembre per {@code ENG-28}.
 * <p>
 * Il mazzo è di una sola fazione: 20 creature della fazione (le Mutant valgono per entrambe le loro fazioni), il
 * Terreno e l'Oggetto della fazione, 6 Spell e 2 Hidden. Ogni carta si sceglie a caso, con la stessa probabilità, tra
 * quelle del suo tipo che hanno ancora copie disponibili e la cui rarità non ha raggiunto il massimo nel mazzo.
 * <p>
 * I limiti sono le proposte dell'API, non ancora decise: copie per carta di Q-02 (Common 3, Uncommon 2, Rare 2,
 * Mythic 1, Legendary 1, Mutant 1; Spell e Hidden contano come Uncommon), massimi per rarità di Q-03 (Legendary 2,
 * Mythic 2, Mutant 1, Rare 5 contando Terreno e Oggetto), al massimo una Hidden {@code DAMAGE} e due {@code SURVIVAL}
 * (Q-24).
 */
public final class BalanceDeckBuilder {

	/** Creature in un mazzo (rule set attivo dell'API). */
	public static final int CREATURES = 20;

	/** Spell in un mazzo. */
	public static final int SPELLS = 6;

	/** Hidden in un mazzo. */
	public static final int HIDDEN = 2;

	private static final int MAX_HIDDEN_DAMAGE = 1;
	private static final int MAX_HIDDEN_SURVIVAL = 2;

	private final List<CardDefinition> catalog;

	/**
	 * Crea il costruttore di mazzi.
	 *
	 * @param catalog carte del catalogo, nell'ordine del CSV: l'ordine conta per il determinismo
	 */
	public BalanceDeckBuilder(List<CardDefinition> catalog) {
		this.catalog = List.copyOf(catalog);
	}

	/**
	 * Costruisce un mazzo da 30 carte di una fazione: prima le creature, poi Terreno, Oggetto, Spell e Hidden. È
	 * l'ordine prima del mescolamento, che fa l'engine.
	 *
	 * @param factionId fazione del mazzo
	 * @param random    generatore creato dal seed della partita
	 * @return lista immutabile di 30 carte
	 * @throws IllegalStateException se il catalogo non basta a riempire il mazzo nei limiti
	 */
	public List<CardDefinition> build(int factionId, RandomGenerator random) {
		DeckCount count = new DeckCount();
		List<CardDefinition> deck = new ArrayList<>();
		CardDefinition terrain = factionCard(CardType.TERRAIN, factionId);
		CardDefinition item = factionCard(CardType.ITEM, factionId);
		count.add(terrain);
		count.add(item);
		deck.addAll(pick(cards(CardType.CREATURE, factionId), CREATURES, random, count));
		deck.add(terrain);
		deck.add(item);
		deck.addAll(pick(cards(CardType.SPELL, null), SPELLS, random, count));
		deck.addAll(pick(cards(CardType.HIDDEN, null), HIDDEN, random, count));
		return List.copyOf(deck);
	}

	/** Copie massime di una carta nel mazzo (Q-02). */
	static int copyLimit(String rarity) {
		return switch (rarity) {
			case "Common" -> 3;
			case "Uncommon", "Rare" -> 2;
			default -> 1;
		};
	}

	/** Massimo di carte di una rarità nel mazzo (Q-03); le altre rarità non hanno massimo. */
	static int maxPerDeck(String rarity) {
		return switch (rarity) {
			case "Legendary", "Mythic" -> 2;
			case "Mutant" -> 1;
			case "Rare" -> 5;
			default -> Integer.MAX_VALUE;
		};
	}

	private List<CardDefinition> pick(List<CardDefinition> pool, int amount, RandomGenerator random,
			DeckCount count) {
		List<CardDefinition> picked = new ArrayList<>();
		for (int i = 0; i < amount; i++) {
			List<CardDefinition> eligible = pool.stream().filter(count::accepts).toList();
			if (eligible.isEmpty()) {
				throw new IllegalStateException("Il catalogo non basta a riempire il mazzo: servono " + amount
						+ " carte, trovate " + picked.size());
			}
			CardDefinition card = eligible.get(random.nextInt(eligible.size()));
			count.add(card);
			picked.add(card);
		}
		return picked;
	}

	/** Carte di un tipo; con una fazione, solo quelle che la hanno (anche come seconda fazione). */
	private List<CardDefinition> cards(CardType type, Integer factionId) {
		return catalog.stream()
				.filter(card -> card.type() == type)
				.filter(card -> factionId == null || card.factions().contains(factionId))
				.toList();
	}

	private CardDefinition factionCard(CardType type, int factionId) {
		List<CardDefinition> cards = cards(type, factionId);
		if (cards.size() != 1) {
			throw new IllegalStateException("Serve un solo " + type + " della fazione " + factionId + ", trovati "
					+ cards.size());
		}
		return cards.getFirst();
	}

	/** Copie per carta, carte per rarità e Hidden per sottotipo già nel mazzo. */
	private static final class DeckCount {

		private final Map<Integer, Integer> copies = new HashMap<>();
		private final Map<String, Integer> rarities = new HashMap<>();
		private final Map<CardSubtype, Integer> hiddenSubtypes = new HashMap<>();

		boolean accepts(CardDefinition card) {
			if (copies.getOrDefault(card.cardId(), 0) >= copyLimit(card.rarity())
					|| rarities.getOrDefault(card.rarity(), 0) >= maxPerDeck(card.rarity())) {
				return false;
			}
			if (card.type() != CardType.HIDDEN) {
				return true;
			}
			int max = card.subtype() == CardSubtype.DAMAGE ? MAX_HIDDEN_DAMAGE : MAX_HIDDEN_SURVIVAL;
			return hiddenSubtypes.getOrDefault(card.subtype(), 0) < max;
		}

		void add(CardDefinition card) {
			copies.merge(card.cardId(), 1, Integer::sum);
			rarities.merge(card.rarity(), 1, Integer::sum);
			if (card.type() == CardType.HIDDEN) {
				hiddenSubtypes.merge(card.subtype(), 1, Integer::sum);
			}
		}
	}
}
