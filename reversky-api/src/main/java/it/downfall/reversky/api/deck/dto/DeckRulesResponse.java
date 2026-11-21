package it.downfall.reversky.api.deck.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.dto.RarityResponse;
import it.downfall.reversky.api.deck.service.DeckRules;

/**
 * Limiti attuali del mazzo (GET /api/v1/decks/rules), così il frontend non li duplica (docs/mazzi).
 *
 * @param rarities limiti di copie ({@code deckLimit}) e massimi per mazzo ({@code maxPerDeck}, null = nessuno) di ogni
 *                 rarità, dalla più comune alla più rara
 */
public record DeckRulesResponse(

	@Schema(example = "30")
	int deckSize,

	@Schema(example = "20")
	int deckCreatures,

	@Schema(example = "6")
	int deckSpells,

	@Schema(example = "2")
	int deckHidden,

	@Schema(example = "1")
	int deckItems,

	@Schema(example = "1")
	int deckTerrains,

	@Schema(example = "2")
	int maxHiddenSurvival,

	@Schema(example = "1")
	int maxHiddenDamage,

	List<RarityResponse> rarities
) {

	public static DeckRulesResponse of(DeckRules rules, List<RarityResponse> rarities) {
		return new DeckRulesResponse(rules.deckSize(), rules.creatures(), rules.spells(), rules.hidden(), rules.items(),
			rules.terrains(), rules.maxHiddenSurvival(), rules.maxHiddenDamage(), rarities);
	}
}
