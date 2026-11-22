package it.downfall.reversky.api.trade.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Accettazione o controproposta di chi non ha il turno (docs/scambi, "Stati e turni").
 */
public class TradeNotYourTurnException extends BusinessException {

	public TradeNotYourTurnException() {
		super(Category.CONFLICT, "TRADE_NOT_YOUR_TURN", "Not your turn", "The other participant has to answer this trade");
	}
}
