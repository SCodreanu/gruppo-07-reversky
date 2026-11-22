package it.downfall.reversky.api.trade.exception;

import java.util.UUID;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Lo scambio non esiste o non riguarda l'utente (docs/scambi): nei due casi la risposta è la stessa, così un estraneo
 * non scopre quali scambi esistono.
 */
public class TradeNotFoundException extends BusinessException {

	public TradeNotFoundException(UUID tradeId) {
		super(Category.NOT_FOUND, "TRADE_NOT_FOUND", "Trade not found", "Trade " + tradeId + " does not exist");
	}
}
