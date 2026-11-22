package it.downfall.reversky.api.trade.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Chiave di {@code trade_offers}: scambio, utente che cede e carta.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeOfferId implements Serializable {

	@Column(name = "trade_id", nullable = false)
	private UUID tradeId;

	@Column(name = "user_id", nullable = false)
	private Integer userId;

	@Column(name = "card_id", nullable = false)
	private Integer cardId;

	public TradeOfferId(UUID tradeId, Integer userId, Integer cardId) {
		this.tradeId = tradeId;
		this.userId = userId;
		this.cardId = cardId;
	}
}
