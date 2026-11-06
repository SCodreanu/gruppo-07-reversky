package it.downfall.reversky.api.auth.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Email o password errate al login.
 * <p>
 * Il messaggio è volutamente generico: non deve far capire se è sbagliata l'email o la password,
 * altrimenti si potrebbe scoprire quali email sono registrate (docs/autenticazione).
 */
public class InvalidCredentialsException extends BusinessException {

	public InvalidCredentialsException() {
		super(Category.UNAUTHENTICATED, "AUTH_INVALID_CREDENTIALS", "Invalid credentials", "Incorrect email or password");
	}
}
