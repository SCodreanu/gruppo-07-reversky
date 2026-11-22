package it.downfall.reversky.api.trade.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * L'altro partecipante non possiede abbastanza copie di una carta richiesta. Si guardano solo le copie possedute e il
 * messaggio non dice quante: non si rivelano le sue copie riservate (docs/scambi, "Proposta").
 */
public class TradeNotOwnedException extends BusinessException {

	public TradeNotOwnedException(Integer cardId) {
		super(Category.RULE_VIOLATION, "TRADE_NOT_OWNED", "Card not owned",
			"The other player does not own enough copies of card " + cardId);
	}
}
