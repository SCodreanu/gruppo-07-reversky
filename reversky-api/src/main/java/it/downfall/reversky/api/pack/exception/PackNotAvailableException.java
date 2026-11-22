package it.downfall.reversky.api.pack.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Il pacchetto esiste ma è disattivato: non si compra (docs/pacchetti). Quelli già comprati restano apribili.
 */
public class PackNotAvailableException extends BusinessException {

	public PackNotAvailableException(Integer packId) {
		super(Category.RULE_VIOLATION, "PACK_NOT_AVAILABLE", "Pack not available",
			"Pack " + packId + " is not on sale");
	}
}
