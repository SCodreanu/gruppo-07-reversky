package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Creazione o modifica di una fazione con un nome già usato da un'altra fazione.
 */
public class FactionNameAlreadyUsedException extends BusinessException {

	public FactionNameAlreadyUsedException(String name) {
		super(Category.CONFLICT, "FACTION_NAME_TAKEN", "Faction name already in use", "The name '" + name + "' is already in use");
	}
}
