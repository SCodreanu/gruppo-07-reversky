package it.downfall.reversky.api.collection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.dto.CatalogCardResponse;

/**
 * Una carta della collezione di un altro utente (GET /users/{id}/collection), per preparare uno scambio: solo la
 * carta e le copie che si possono chiedere (deciso il 20 novembre).
 */
public record AvailableCardResponse(

	@Schema(description = "The card, in the same shape as GET /cards/{id}")
	CatalogCardResponse card,

	@Schema(example = "2", description = "Copies that can be asked for in a trade: owned minus those reserved by the "
		+ "user's open offers. It can be 0")
	int availableQuantity
) {
}
