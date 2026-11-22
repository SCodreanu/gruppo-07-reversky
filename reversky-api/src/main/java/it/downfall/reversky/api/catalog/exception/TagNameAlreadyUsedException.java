package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Creazione o modifica di un tag con un nome già usato da un altro tag ({@code uq_tags_name}).
 */
public class TagNameAlreadyUsedException extends BusinessException {

	public TagNameAlreadyUsedException(String name) {
		super(Category.CONFLICT, "TAG_NAME_TAKEN", "Tag name already in use", "The name '" + name + "' is already in use");
	}
}
