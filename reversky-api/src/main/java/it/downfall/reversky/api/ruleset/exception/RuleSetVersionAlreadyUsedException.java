package it.downfall.reversky.api.ruleset.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Creazione o modifica di un rule set con una versione già usata da un altro rule set.
 */
public class RuleSetVersionAlreadyUsedException extends BusinessException {

	public RuleSetVersionAlreadyUsedException(String version) {
		super(Category.CONFLICT, "RULE_SET_VERSION_TAKEN", "Rule set version already in use",
			"The version '" + version + "' is already in use");
	}
}