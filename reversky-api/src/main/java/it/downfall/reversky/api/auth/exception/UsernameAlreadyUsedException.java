package it.downfall.reversky.api.auth.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Registrazione con uno username già usato da un altro utente.
 */
public class UsernameAlreadyUsedException extends BusinessException {

	public UsernameAlreadyUsedException(String username) {
		super(Category.CONFLICT, "AUTH_USERNAME_TAKEN", "Username already in use", "The username '" + username + "' is already in use");
	}
}
