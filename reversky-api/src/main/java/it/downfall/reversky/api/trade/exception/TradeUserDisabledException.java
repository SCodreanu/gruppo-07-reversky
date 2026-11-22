package it.downfall.reversky.api.trade.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * L'altro partecipante è stato disattivato mentre lo scambio era aperto: non si accetta, e lo scambio resta aperto
 * (docs/scambi, "Accettazione"). Si può ancora rifiutare.
 */
public class TradeUserDisabledException extends BusinessException {

	public TradeUserDisabledException() {
		super(Category.RULE_VIOLATION, "TRADE_USER_DISABLED", "Other player disabled",
			"The other player's account is disabled: the trade cannot be accepted");
	}
}
