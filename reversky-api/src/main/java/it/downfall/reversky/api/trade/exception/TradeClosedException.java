package it.downfall.reversky.api.trade.exception;

import it.downfall.reversky.api.common.exception.BusinessException;
import it.downfall.reversky.api.trade.entity.TradeStatus;

/**
 * Azione su uno scambio già accettato o rifiutato (docs/scambi, "Stati e turni").
 */
public class TradeClosedException extends BusinessException {

	public TradeClosedException(TradeStatus status) {
		super(Category.CONFLICT, "TRADE_CLOSED", "Trade closed", "The trade is already " + status);
	}
}
