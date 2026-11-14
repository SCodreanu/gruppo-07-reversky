package it.downfall.reversky.api.ruleset.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Nessun rule set attivo quando il client chiama GET /rules.
 */
public class NoActiveRuleSetException extends BusinessException {

	public NoActiveRuleSetException() {
		super(Category.NOT_FOUND, "NO_ACTIVE_RULE_SET", "No active rule set",
			"There is no active rule set");
	}
}