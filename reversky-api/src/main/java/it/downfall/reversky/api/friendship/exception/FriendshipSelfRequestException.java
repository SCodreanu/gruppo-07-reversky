package it.downfall.reversky.api.friendship.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Richiesta di amicizia verso se stessi (docs/amicizie, regola 1): corrisponde al vincolo
 * {@code chk_friendships_no_self_friendship}, qui tradotto in 400.
 */
public class FriendshipSelfRequestException extends BusinessException {

	public FriendshipSelfRequestException() {
		super(Category.INVALID_REQUEST, "FRIENDSHIP_SELF_REQUEST",
			"Cannot be friend with yourself",
			"You cannot send a friend request to yourself");
	}
}