package it.downfall.reversky.api.auth.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Richiesta di un nuovo link di verifica per un'email già verificata.
 */
public class EmailAlreadyVerifiedException extends BusinessException {

	public EmailAlreadyVerifiedException() {
		super(Category.CONFLICT, "AUTH_EMAIL_ALREADY_VERIFIED", "Email already verified", "The email of this account is already verified");
	}
}
