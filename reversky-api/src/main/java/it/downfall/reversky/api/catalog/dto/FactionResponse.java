package it.downfall.reversky.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.entity.Faction;

/**
 * Fazione restituita da GET /factions e dagli endpoint di /admin/factions.
 */
public record FactionResponse(

	@Schema(example = "1")
	Integer id,

	@Schema(example = "Theory of the Wound")
	String name,

	@Schema(example = "factions/wound.svg")
	String emblemPath,

	@Schema(example = "Those who survived the Backrooms carry its wounds.")
	String description
) {

	/**
	 * Converte l'entità nel DTO.
	 */
	public static FactionResponse from(Faction faction) {
		return new FactionResponse(faction.getId(), faction.getName(), faction.getEmblemPath(), faction.getDescription());
	}
}
