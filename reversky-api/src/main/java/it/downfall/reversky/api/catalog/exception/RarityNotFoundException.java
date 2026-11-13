package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Rarità inesistente (PUT su /admin/rarities/{id}).
 */
public class RarityNotFoundException extends BusinessException {

	public RarityNotFoundException() {
		super(Category.NOT_FOUND, "RARITY_NOT_FOUND", "Rarity not found", "This rarity does not exist");
	}
}
