package it.downfall.reversky.api.ruleset.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Due attivazioni contemporanee quando non c'era nessun rule set attivo: la seconda viola
 * {@code uq_rule_sets_one_active} e viene annullata. Basta ripetere la richiesta.
 */
public class RuleSetActivationConflictException extends BusinessException {

	public RuleSetActivationConflictException() {
		super(Category.CONFLICT, "RULE_SET_ACTIVATION_CONFLICT", "Rule set activation conflict",
			"Another rule set was activated at the same time; please retry");
	}
}
