package it.downfall.reversky.api.battle.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Le sfide amichevoli sono solo tra amici (docs/sfide): l'invito o l'accettazione non avviene.
 */
public class NotFriendsException extends BusinessException {

	public NotFriendsException() {
		super(Category.FORBIDDEN, "NOT_FRIENDS", "Not friends",
			"You can challenge only your friends");
	}
}
