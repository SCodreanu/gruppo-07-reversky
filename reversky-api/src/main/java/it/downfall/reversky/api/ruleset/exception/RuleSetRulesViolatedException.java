package it.downfall.reversky.api.ruleset.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Regola di gioco violata (es. la somma delle parti del mazzo non torna).
 */
public class RuleSetRulesViolatedException extends BusinessException {

	public RuleSetRulesViolatedException(String detail) {
		super(Category.RULE_VIOLATION, "RULE_SET_RULES_VIOLATED", "Rule set rules violated", detail);
	}
}