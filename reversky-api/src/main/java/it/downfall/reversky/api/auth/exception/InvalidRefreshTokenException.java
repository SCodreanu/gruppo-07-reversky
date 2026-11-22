package it.downfall.reversky.api.auth.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Refresh token inesistente o scaduto durante il rinnovo.
 * <p>
 * È una credenziale, quindi la risposta è 401: il frontend deve riportare l'utente al login.
 * Il logout non la lancia: con un token sconosciuto risponde comunque 204.
 */
public class InvalidRefreshTokenException extends BusinessException {

	public InvalidRefreshTokenException() {
		super(Category.UNAUTHENTICATED, "AUTH_REFRESH_TOKEN_INVALID", "Invalid session", "Your session has expired or is invalid: please log in again");
	}
}
