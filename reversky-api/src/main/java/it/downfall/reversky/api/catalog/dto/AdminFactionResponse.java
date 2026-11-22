package it.downfall.reversky.api.catalog.dto;

import java.util.Comparator;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.entity.Faction;

/**
 * Fazione restituita dagli endpoint di /admin/factions: come {@link FactionResponse}, più i tag (API-13), che servono
 * solo al backoffice per i filtri dei pacchetti. {@code GET /factions} pubblico resta senza tag.
 *
 * @param tags tag della fazione in ordine di nome; lista vuota se non ne ha
 */
public record AdminFactionResponse(

	@Schema(example = "1")
	Integer id,

	@Schema(example = "Theory of the Wound")
	String name,

	@Schema(example = "factions/wound.svg")
	String emblemPath,

	@Schema(example = "Those who survived the Backrooms carry its wounds.")
	String description,

	List<TagResponse> tags
) {

	/**
	 * Converte l'entità nel DTO; i tag devono essere caricabili (dentro la transazione del service).
	 */
	public static AdminFactionResponse from(Faction faction) {
		List<TagResponse> tags = faction.getTags().stream()
			.map(TagResponse::from)
			.sorted(Comparator.comparing(TagResponse::name))
			.toList();
		return new AdminFactionResponse(faction.getId(), faction.getName(), faction.getEmblemPath(),
			faction.getDescription(), tags);
	}
}
