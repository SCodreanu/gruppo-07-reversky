package it.downfall.reversky.api.user.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Utente inesistente.
 */
public class UserNotFoundException extends BusinessException {

	public UserNotFoundException() {
		super(Category.NOT_FOUND, "USER_NOT_FOUND", "User not found", "This user does not exist");
	}
}
