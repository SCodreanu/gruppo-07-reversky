package it.downfall.reversky.api.trade.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * La stessa carta sta sia in {@code give} sia in {@code receive} (deciso il 22 novembre): le copie andrebbero avanti e
 * indietro, quasi sempre è un errore del client.
 */
public class TradeSameCardBothSidesException extends BusinessException {

	public TradeSameCardBothSidesException(Integer cardId) {
		super(Category.RULE_VIOLATION, "TRADE_SAME_CARD_BOTH_SIDES", "Same card on both sides",
			"The card " + cardId + " cannot be both given and received");
	}
}
