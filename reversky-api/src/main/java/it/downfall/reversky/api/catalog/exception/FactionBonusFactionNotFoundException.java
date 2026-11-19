package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Una delle due fazioni indicate nella richiesta non esiste (422: la richiesta è
 * malformata, non l'URL).
 */
public class FactionBonusFactionNotFoundException extends BusinessException {

	public FactionBonusFactionNotFoundException(Integer factionId) {
		super(Category.RULE_VIOLATION, "FACTION_BONUS_FACTION_NOT_FOUND",
			"Faction not found in the bonus",
			"Faction " + factionId + " does not exist");
	}
}
