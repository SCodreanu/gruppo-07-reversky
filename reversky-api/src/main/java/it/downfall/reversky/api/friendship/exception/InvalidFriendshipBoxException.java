package it.downfall.reversky.api.friendship.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * GET /friends/requests con {@code box} diverso da {@code incoming} o {@code outgoing}.
 */
public class InvalidFriendshipBoxException extends BusinessException {

	public InvalidFriendshipBoxException(String box) {
		super(Category.INVALID_REQUEST, "INVALID_FRIENDSHIP_BOX",
			"Invalid friend requests box",
			"The box parameter must be 'incoming' or 'outgoing' (got '" + box + "')");
	}
}