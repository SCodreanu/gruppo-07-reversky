package it.downfall.reversky.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.entity.Faction;

/**
 * Fazione in forma breve, dentro una carta ({@link CatalogCardResponse}): stessa forma di docs/collezione.
 * Per emblema e descrizione c'è {@link FactionResponse}.
 */
public record FactionSummaryResponse(

	@Schema(example = "2")
	Integer id,

	@Schema(example = "Theory of the Invitation")
	String name
) {

	/**
	 * Converte l'entità nel DTO. Se la fazione è un proxy LAZY, {@code getName()} la carica:
	 * va chiamato dentro la transazione del service.
	 *
	 * @return null se la carta non ha questa fazione (Spell e Hidden, o seconda fazione assente)
	 */
	public static FactionSummaryResponse from(Faction faction) {
		return faction == null ? null : new FactionSummaryResponse(faction.getId(), faction.getName());
	}
}
