package it.downfall.reversky.api.auth.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Token di reset della password inesistente o scaduto.
 * <p>
 * Token inesistente e token scaduto danno lo stesso errore: distinguerli non aiuta l'utente
 * e darebbe informazioni a chi prova token a caso.
 */
public class InvalidPasswordResetTokenException extends BusinessException {

	public InvalidPasswordResetTokenException() {
		super(Category.INVALID_REQUEST, "AUTH_RESET_TOKEN_INVALID", "Invalid link", "The password reset link is invalid or has expired");
	}
}
