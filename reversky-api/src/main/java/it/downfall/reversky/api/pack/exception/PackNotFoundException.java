package it.downfall.reversky.api.pack.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Il tipo di pacchetto non esiste (docs/pacchetti).
 */
public class PackNotFoundException extends BusinessException {

	public PackNotFoundException(Integer packId) {
		super(Category.NOT_FOUND, "PACK_NOT_FOUND", "Pack not found",
			"Pack " + packId + " does not exist");
	}
}
