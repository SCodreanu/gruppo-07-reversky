package it.downfall.reversky.api.user.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Un admin ha provato a disattivare se stesso o a togliersi il ruolo ADMIN (docs/backoffice, "Utenti"):
 * il backoffice potrebbe restare senza nessun admin.
 */
public class AdminSelfActionException extends BusinessException {

	public AdminSelfActionException(String detail) {
		super(Category.RULE_VIOLATION, "ADMIN_SELF_ACTION", "Action not allowed on your own account", detail);
	}
}
