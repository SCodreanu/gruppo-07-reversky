package it.downfall.reversky.api.trade.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Il parametro box di GET /trades non è incoming né outgoing.
 */
public class InvalidTradeBoxException extends BusinessException {

	public InvalidTradeBoxException(String box) {
		super(Category.INVALID_REQUEST, "INVALID_TRADE_BOX", "Invalid box",
			"Unknown box " + box + ": use incoming or outgoing");
	}
}
