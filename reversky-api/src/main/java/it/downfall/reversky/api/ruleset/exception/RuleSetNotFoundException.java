package it.downfall.reversky.api.ruleset.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Rule set inesistente (PUT o activate su id sconosciuto).
 */
public class RuleSetNotFoundException extends BusinessException {

	public RuleSetNotFoundException() {
		super(Category.NOT_FOUND, "RULE_SET_NOT_FOUND", "Rule set not found", "This rule set does not exist");
	}
}