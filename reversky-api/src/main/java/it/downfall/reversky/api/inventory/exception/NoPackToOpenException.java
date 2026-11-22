package it.downfall.reversky.api.inventory.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * L'utente non ha pacchetti di quel tipo da aprire (docs/pacchetti): niente cambia.
 */
public class NoPackToOpenException extends BusinessException {

	public NoPackToOpenException(Integer packId) {
		super(Category.RULE_VIOLATION, "NO_PACK_TO_OPEN", "No pack to open",
			"You have no pack " + packId + " to open");
	}
}
