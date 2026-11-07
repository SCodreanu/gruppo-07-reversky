package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Creazione o modifica di una rarità con un nome già usato da un'altra rarità.
 */
public class RarityNameAlreadyUsedException extends BusinessException {

	public RarityNameAlreadyUsedException(String name) {
		super(Category.CONFLICT, "RARITY_NAME_TAKEN", "Rarity name already in use", "The name '" + name + "' is already in use");
	}
}
