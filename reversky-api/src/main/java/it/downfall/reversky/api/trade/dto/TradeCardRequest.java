package it.downfall.reversky.api.trade.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Le copie di una carta in un lato di una proposta.
 */
public record TradeCardRequest(

	@Schema(example = "4")
	@NotNull(message = "Card id is required")
	Integer cardId,

	@Schema(example = "3")
	@NotNull(message = "Quantity is required")
	@Min(value = 1, message = "Quantity must be at least 1")
	Integer quantity
) {

	/**
	 * Nessuna carta ripetuta nel lato. Una lista nulla vale come valida: la segnala {@code @NotNull} del campo.
	 */
	static boolean hasDistinctCards(List<TradeCardRequest> side) {
		if (side == null) {
			return true;
		}
		Set<Integer> seen = new HashSet<>();
		return side.stream()
			.filter(card -> card != null && card.cardId() != null)
			.allMatch(card -> seen.add(card.cardId()));
	}
}
