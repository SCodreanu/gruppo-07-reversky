package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Fazione inesistente (GET o PUT su /admin/factions/{id}).
 */
public class FactionNotFoundException extends BusinessException {

	public FactionNotFoundException() {
		super(Category.NOT_FOUND, "FACTION_NOT_FOUND", "Faction not found", "This faction does not exist");
	}
}
