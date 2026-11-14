package it.downfall.reversky.api.ruleset.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * PUT su un rule set già usato da una partita: non si modifica, si crea una nuova versione.
 */
public class RuleSetInUseException extends BusinessException {

	public RuleSetInUseException() {
		super(Category.CONFLICT, "RULE_SET_IN_USE", "Rule set already used by a battle",
			"This rule set has already been used by a battle and cannot be modified; create a new version instead");
	}
}