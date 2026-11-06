package it.downfall.reversky.api.auth.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Token di verifica dell'email inesistente o scaduto.
 */
public class InvalidEmailVerificationTokenException extends BusinessException {

	public InvalidEmailVerificationTokenException() {
		super(Category.INVALID_REQUEST, "AUTH_VERIFICATION_TOKEN_INVALID", "Invalid link", "The verification link is invalid or has expired");
	}
}
