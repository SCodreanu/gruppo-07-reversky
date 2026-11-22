package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Carta inesistente (GET /cards/{id} e gli endpoint di /admin/cards/{id}).
 */
public class CatalogCardNotFoundException extends BusinessException {

	public CatalogCardNotFoundException() {
		super(Category.NOT_FOUND, "CARD_NOT_FOUND", "Card not found", "This card does not exist");
	}
}
