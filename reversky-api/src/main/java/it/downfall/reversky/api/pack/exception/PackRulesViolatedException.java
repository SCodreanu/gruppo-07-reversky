package it.downfall.reversky.api.pack.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Slot e bonus non rispettano le regole del pacchetto, ad esempio un bonus non più raro di tutti gli slot base (docs/backoffice, "Pacchetti").
 */
public class PackRulesViolatedException extends BusinessException {

	public PackRulesViolatedException(String detail) {
		super(Category.RULE_VIOLATION, "PACK_RULES_VIOLATED", "Pack rules violated",
			detail);
	}
}
