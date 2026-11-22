package it.downfall.reversky.api.trade.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Un utente prova a proporre uno scambio a se stesso (chk_trades_no_self_trade, docs/scambi "Proposta").
 */
public class TradeSelfException extends BusinessException {

	public TradeSelfException() {
		super(Category.RULE_VIOLATION, "TRADE_SELF", "You cannot trade with yourself", "You cannot trade with yourself");
	}
}
