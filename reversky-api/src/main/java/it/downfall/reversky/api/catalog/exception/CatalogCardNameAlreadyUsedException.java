package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Nome già usato da un'altra carta originale (docs/backoffice, "Carte", regola 7).
 * <p>
 * Una variante può riusare il nome della propria originale, non quello di un'altra.
 */
public class CatalogCardNameAlreadyUsedException extends BusinessException {

	public CatalogCardNameAlreadyUsedException(String name) {
		super(Category.CONFLICT, "CARD_NAME_TAKEN", "Card name already in use", "The name '" + name + "' is already used by another original card");
	}
}
