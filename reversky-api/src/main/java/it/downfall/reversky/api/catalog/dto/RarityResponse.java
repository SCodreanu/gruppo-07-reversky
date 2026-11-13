package it.downfall.reversky.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.entity.Rarity;

/**
 * Rarità restituita da GET /rarities e dagli endpoint di /admin/rarities.
 * <p>
 * maxPerDeck a null significa "nessun limite".
 */
public record RarityResponse(

	@Schema(example = "3")
	Integer id,

	@Schema(example = "Rare")
	String name,

	@Schema(example = "2")
	Integer deckLimit,

	@Schema(example = "5", description = "Null means no limit")
	Integer maxPerDeck,

	@Schema(example = "3", description = "Position from common to rare")
	Integer sortingOrder
) {

	/**
	 * Converte l'entità nel DTO.
	 */
	public static RarityResponse from(Rarity rarity) {
		return new RarityResponse(
			rarity.getId(),
			rarity.getName(),
			rarity.getDeckLimit(),
			rarity.getMaxPerDeck(),
			rarity.getSortingOrder());
	}
}
