package it.downfall.reversky.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.entity.Rarity;

/**
 * Rarità in forma breve, dentro una carta ({@link CatalogCardResponse}): stessa forma di
 * docs/collezione. Per i limiti completi c'è {@link RarityResponse}.
 */
public record RaritySummaryResponse(

	@Schema(example = "5")
	Integer id,

	@Schema(example = "Legendary")
	String name
) {

	/**
	 * Converte l'entità nel DTO. Se la rarità è un proxy LAZY, {@code getName()} la carica:
	 * va chiamato dentro la transazione del service.
	 */
	public static RaritySummaryResponse from(Rarity rarity) {
		return new RaritySummaryResponse(rarity.getId(), rarity.getName());
	}
}
