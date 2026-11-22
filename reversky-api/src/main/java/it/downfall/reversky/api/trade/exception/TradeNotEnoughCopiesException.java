package it.downfall.reversky.api.trade.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Chi propone, controproponde o accetta non ha abbastanza copie <b>disponibili</b> di una carta che cede: possedute
 * meno quelle riservate dalle sue offerte aperte (docs/scambi, "Copie riservate").
 */
public class TradeNotEnoughCopiesException extends BusinessException {

	public TradeNotEnoughCopiesException(Integer cardId, int available, int offered) {
		super(Category.RULE_VIOLATION, "TRADE_NOT_ENOUGH_COPIES", "Not enough copies",
			"You have " + available + " available copies of card " + cardId + ", " + offered + " offered");
	}
}
