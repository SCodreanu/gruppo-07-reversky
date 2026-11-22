package it.downfall.reversky.api.inventory.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Il saldo non basta per l'operazione (docs/crediti, regola 1): il saldo resta invariato.
 */
public class InsufficientCreditsException extends BusinessException {

	public InsufficientCreditsException(int required, int available) {
		super(Category.RULE_VIOLATION, "INSUFFICIENT_CREDITS", "Insufficient credits",
			"Insufficient credits: " + required + " required, " + available + " available");
	}
}
