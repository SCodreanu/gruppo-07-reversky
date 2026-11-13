package it.downfall.reversky.api.auth.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Registrazione con un'email già usata da un altro utente.
 * <p>
 * L'email non viene ripetuta nel messaggio: è un dato personale e finirebbe nei log.
 */
public class EmailAlreadyUsedException extends BusinessException {

	public EmailAlreadyUsedException() {
		super(Category.CONFLICT, "AUTH_EMAIL_TAKEN", "Email already in use", "An account with this email already exists");
	}
}
