package it.downfall.reversky.api.pack.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.dto.CatalogCardResponse;

/**
 * Esito dell'apertura di un pacchetto (POST /api/v1/inventory/packs/{packId}/open, docs/pacchetti).
 *
 * @param remainingPacks pacchetti di quel tipo ancora da aprire
 * @param cards          le carte ricevute nell'ordine degli slot, dalla più comune: il frontend ci fa l'animazione
 */
public record OpenPackResponse(

	@Schema(example = "1")
	Integer packId,

	@Schema(example = "2")
	int remainingPacks,

	List<OpenedCardResponse> cards
) {

	/**
	 * Una carta ricevuta.
	 *
	 * @param isNew true se l'utente non la possedeva prima di questa apertura; se la stessa carta nuova esce due volte,
	 *              solo la prima è nuova
	 */
	public record OpenedCardResponse(

		CatalogCardResponse card,

		@Schema(example = "true")
		boolean isNew
	) {
	}
}
