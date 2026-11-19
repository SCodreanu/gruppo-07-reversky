package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * La stessa coppia {@code (origin, target)} compare più di una volta nel body del PUT.
 * La chiave composta del database la rifiuterebbe come 500: qui diventa 422.
 */
public class FactionBonusDuplicateException extends BusinessException {

	public FactionBonusDuplicateException(Integer originFactionId, Integer targetFactionId) {
		super(Category.RULE_VIOLATION, "FACTION_BONUS_DUPLICATE",
			"Duplicate faction bonus",
			"The pair (" + originFactionId + ", " + targetFactionId + ") appears more than once");
	}
}
