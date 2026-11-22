package it.downfall.reversky.api.trade.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Proposta o controproposta senza nessuna carta: un lato può essere vuoto (regalo o richiesta, Q-09), non entrambi.
 */
public class TradeEmptyException extends BusinessException {

	public TradeEmptyException() {
		super(Category.RULE_VIOLATION, "TRADE_EMPTY", "Empty trade", "A trade needs at least one card");
	}
}
