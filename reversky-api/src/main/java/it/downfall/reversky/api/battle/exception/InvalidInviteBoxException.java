package it.downfall.reversky.api.battle.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Il parametro box di GET /battle-invites non è incoming né outgoing.
 */
public class InvalidInviteBoxException extends BusinessException {

	public InvalidInviteBoxException(String box) {
		super(Category.INVALID_REQUEST, "INVALID_BOX", "Invalid box",
			"Unknown box " + box + ": use incoming or outgoing");
	}
}
