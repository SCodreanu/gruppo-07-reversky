package it.downfall.reversky.api.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * L'altro partecipante di uno scambio.
 */
public record TradeUserResponse(

	@Schema(example = "12")
	Integer id,

	@Schema(example = "bob")
	String username
) {
}
