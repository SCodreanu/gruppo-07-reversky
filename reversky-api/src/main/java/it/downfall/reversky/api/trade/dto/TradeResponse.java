package it.downfall.reversky.api.trade.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.trade.entity.TradeStatus;

/**
 * Uno scambio dal punto di vista di chi guarda (docs/scambi, deciso il 22 novembre).
 *
 * @param other  l'altro partecipante
 * @param myTurn tocca a me accettare o controproporre; sempre false su uno scambio chiuso
 */
public record TradeResponse(

	UUID id,

	TradeUserResponse other,

	@Schema(example = "WAITING_RECIPIENT")
	TradeStatus status,

	@Schema(example = "true")
	boolean myTurn,

	Instant createdAt,

	Instant updatedAt
) {
}
