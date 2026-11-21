package it.downfall.reversky.api.deck.service;

import it.downfall.reversky.api.ruleset.entity.RuleSet;

/**
 * Composizione del mazzo dal rule set attivo (docs/mazzi, "Composizione"): mai valori nel codice.
 * <p>
 * I limiti di copie e di bilanciamento non sono qui: stanno nelle rarità delle carte ({@code deck_limit},
 * {@code max_per_deck}) e nel {@code copy_limit} di ogni carta.
 */
public record DeckRules(
	int deckSize,
	int creatures,
	int spells,
	int hidden,
	int items,
	int terrains,
	int maxHiddenSurvival,
	int maxHiddenDamage
) {

	public static DeckRules from(RuleSet ruleSet) {
		return new DeckRules(ruleSet.getDeckSize(), ruleSet.getDeckCreatures(), ruleSet.getDeckSpells(),
			ruleSet.getDeckHidden(), ruleSet.getDeckItems(), ruleSet.getDeckTerrains(), ruleSet.getMaxHiddenSurvival(),
			ruleSet.getMaxHiddenDamage());
	}
}
