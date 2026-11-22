package it.downfall.reversky.api.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.dto.CatalogCardResponse;

/**
 * Le copie di una carta in un lato dello scambio, con la carta completa come in GET /collection (deciso il 22
 * novembre).
 */
public record TradeCardResponse(

	CatalogCardResponse card,

	@Schema(example = "3")
	int quantity
) {
}
