package it.downfall.reversky.api.support;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.ruleset.entity.RuleSet;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.user.entity.User;

/**
 * Contenuti di mazzo validi per il rule set attivo (docs/test, "Dati di test"), con le carte già nella collezione
 * dell'utente.
 * <p>
 * Come {@link TestCards} non è un {@code @TestComponent}, per non cambiare la configurazione del test: si crea con
 * {@code new TestDecks(testCards, testCollections, ruleSetService)}.
 * <p>
 * Ogni chiamata crea carte nuove: creature Common con al massimo 3 copie (il {@code deck_limit} della Common del
 * seed), Spell e Hidden Rare con al massimo 2 copie (il {@code deck_limit} della Rare; non contano nel
 * {@code max_per_deck}), Terreni e Oggetti Rare con una copia. Le Hidden sono {@code SURVIVAL}: basta finché
 * {@code deck_hidden} non supera {@code max_hidden_survival}, come nel rule set {@code 1.0}.
 */
public class TestDecks {

	private static final int COMMON_COPIES = 3;
	private static final int RARE_COPIES = 2;

	private final TestCards testCards;
	private final TestCollections testCollections;
	private final RuleSetService ruleSetService;

	public TestDecks(TestCards testCards, TestCollections testCollections, RuleSetService ruleSetService) {
		this.testCards = testCards;
		this.testCollections = testCollections;
		this.ruleSetService = ruleSetService;
	}

	/**
	 * Un contenuto valido per il rule set attivo, con tutte le carte date all'utente.
	 *
	 * @return copie per carta, nell'ordine di creazione
	 */
	public Map<CatalogCard, Integer> validContent(User user) {
		RuleSet rules = ruleSetService.getActiveRuleSet();
		Map<CatalogCard, Integer> content = new LinkedHashMap<>();

		fill(content, CardType.CREATURE, rules.getDeckCreatures(), COMMON_COPIES);
		fill(content, CardType.SPELL, rules.getDeckSpells(), RARE_COPIES);
		fill(content, CardType.HIDDEN, rules.getDeckHidden(), RARE_COPIES);
		fill(content, CardType.ITEM, rules.getDeckItems(), 1);
		fill(content, CardType.TERRAIN, rules.getDeckTerrains(), 1);

		content.forEach((card, quantity) -> testCollections.give(user, card, quantity));
		return content;
	}

	/**
	 * Il JSON di {@code cards} per POST e PUT /decks.
	 */
	public static String toJson(Map<CatalogCard, Integer> content) {
		return content.entrySet().stream()
			.map(entry -> "{\"cardId\": %d, \"quantity\": %d}".formatted(entry.getKey().getId(), entry.getValue()))
			.collect(Collectors.joining(", ", "[", "]"));
	}

	/**
	 * Il JSON completo di POST e PUT /decks.
	 */
	public static String requestJson(String name, Map<CatalogCard, Integer> content) {
		return "{\"name\": \"" + name + "\", \"cards\": " + toJson(content) + "}";
	}

	private void fill(Map<CatalogCard, Integer> content, CardType type, int count, int maxCopies) {
		int remaining = count;
		while (remaining > 0) {
			int copies = Math.min(maxCopies, remaining);
			content.put(testCards.create(TestCards.uniqueName(type.name()), type), copies);
			remaining -= copies;
		}
	}
}
