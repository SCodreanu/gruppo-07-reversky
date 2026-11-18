package it.downfall.reversky.api.friendship.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Non esiste una richiesta PENDING tra i due utenti (accept/reject).
 * <p>
 * Copre anche il caso "non sono il destinatario": la riga cercata è {@code (requesterId, currentUserId)},
 * quindi se io non sono il recipient non c'è niente da accettare o rifiutare.
 */
public class FriendshipRequestNotFoundException extends BusinessException {

	public FriendshipRequestNotFoundException() {
		super(Category.NOT_FOUND, "FRIENDSHIP_REQUEST_NOT_FOUND",
			"Friend request not found",
			"No pending friend request from this user");
	}
}