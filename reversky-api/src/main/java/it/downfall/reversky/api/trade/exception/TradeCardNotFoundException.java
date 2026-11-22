package it.downfall.reversky.api.trade.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Una carta della proposta non esiste nel catalogo. {@code 422} e non {@code 404}: lo scambio è giusto, è sbagliato un
 * riferimento nel corpo.
 */
public class TradeCardNotFoundException extends BusinessException {

	public TradeCardNotFoundException(Integer cardId) {
		super(Category.RULE_VIOLATION, "TRADE_CARD_NOT_FOUND", "Card not found", "The card " + cardId + " does not exist");
	}
}
