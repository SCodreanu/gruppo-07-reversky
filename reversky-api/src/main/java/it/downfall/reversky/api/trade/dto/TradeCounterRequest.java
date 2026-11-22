package it.downfall.reversky.api.trade.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Una controproposta (POST /api/v1/trades/{id}/counter, docs/scambi "Controproposta"): sostituisce tutte le offerte,
 * dal punto di vista di chi controproponde. Stessi controlli di forma di {@link TradeRequest}.
 *
 * @param give    le carte che cedo, anche vuota
 * @param receive le carte che chiedo, anche vuota
 */
public record TradeCounterRequest(

	@NotNull(message = "Give is required (use an empty list for none)")
	List<@NotNull(message = "Card must not be null") @Valid TradeCardRequest> give,

	@NotNull(message = "Receive is required (use an empty list for none)")
	List<@NotNull(message = "Card must not be null") @Valid TradeCardRequest> receive
) {

	@Schema(hidden = true)
	@AssertTrue(message = "A card can appear only once in give")
	public boolean isGiveDistinct() {
		return TradeCardRequest.hasDistinctCards(give);
	}

	@Schema(hidden = true)
	@AssertTrue(message = "A card can appear only once in receive")
	public boolean isReceiveDistinct() {
		return TradeCardRequest.hasDistinctCards(receive);
	}
}
