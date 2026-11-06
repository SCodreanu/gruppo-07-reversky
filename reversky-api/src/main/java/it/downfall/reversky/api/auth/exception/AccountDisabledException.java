package it.downfall.reversky.api.auth.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Login di un utente con {@code is_enabled = false} (disattivato dal backoffice).
 */
public class AccountDisabledException extends BusinessException {

	public AccountDisabledException() {
		super(Category.FORBIDDEN, "AUTH_ACCOUNT_DISABLED", "Account disabled", "This account has been disabled");
	}
}
