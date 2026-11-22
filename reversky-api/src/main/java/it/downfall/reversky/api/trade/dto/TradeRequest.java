package it.downfall.reversky.api.trade.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Una proposta di scambio (POST /api/v1/trades, docs/scambi "Proposta"), dal punto di vista di chi propone.
 * <p>
 * Qui si controlla solo la forma (400): quantità ≥ 1 e nessuna carta ripetuta nello stesso lato. Le regole che
 * dipendono dal database (carte esistenti, copie disponibili, destinatario) le controlla il service (404 e 422).
 *
 * @param give    le carte che cedo, anche vuota (una richiesta)
 * @param receive le carte che chiedo, anche vuota (un regalo)
 */
public record TradeRequest(

	@Schema(example = "12")
	@NotNull(message = "Recipient id is required")
	Integer recipientId,

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
