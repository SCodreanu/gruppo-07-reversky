package it.downfall.reversky.api.friendship.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * DELETE /friends/{userId} su una relazione inesistente.
 */
public class FriendshipNotFoundException extends BusinessException {

	public FriendshipNotFoundException() {
		super(Category.NOT_FOUND, "FRIENDSHIP_NOT_FOUND",
			"Friendship not found",
			"There is no friendship or friend request with this user");
	}
}