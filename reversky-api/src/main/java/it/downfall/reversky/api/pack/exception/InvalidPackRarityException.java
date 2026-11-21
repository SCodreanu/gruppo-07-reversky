package it.downfall.reversky.api.pack.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Uno slot o un bonus indica una rarità che non esiste (docs/backoffice, "Pacchetti").
 */
public class InvalidPackRarityException extends BusinessException {

	public InvalidPackRarityException(Integer rarityId) {
		super(Category.RULE_VIOLATION, "PACK_RARITY_INVALID", "Invalid pack rarity",
			"Rarity " + rarityId + " does not exist");
	}
}
