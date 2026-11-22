package it.downfall.reversky.api.pack.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Un altro pacchetto ha già questo nome (vincolo {@code uq_packs_name}, docs/backoffice).
 */
public class PackNameAlreadyUsedException extends BusinessException {

	public PackNameAlreadyUsedException(String name) {
		super(Category.CONFLICT, "PACK_NAME_ALREADY_USED", "Pack name already used",
			"A pack named '" + name + "' already exists");
	}
}
