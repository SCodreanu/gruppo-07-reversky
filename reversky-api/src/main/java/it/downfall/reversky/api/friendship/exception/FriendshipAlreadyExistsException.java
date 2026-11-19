package it.downfall.reversky.api.friendship.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Esiste già una relazione in stato incompatibile con la richiesta (docs/amicizie, regole 3 e 5):
 * amicizia ACCEPTED, richiesta PENDING nella stessa direzione, o violazione del vincolo
 * {@code uq_friendships_pair} per due richieste opposte concorrenti.
 */
public class FriendshipAlreadyExistsException extends BusinessException {

	public FriendshipAlreadyExistsException() {
		super(Category.CONFLICT, "FRIENDSHIP_ALREADY_EXISTS",
			"Friendship already exists",
			"A friendship or friend request already exists between these users");
	}
}