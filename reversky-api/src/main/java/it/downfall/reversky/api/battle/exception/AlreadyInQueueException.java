package it.downfall.reversky.api.battle.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * L'utente ha già un ticket nella coda classificata (chiave primaria di {@code matchmaking_queue}, docs/sfide).
 */
public class AlreadyInQueueException extends BusinessException {

	public AlreadyInQueueException() {
		super(Category.CONFLICT, "ALREADY_IN_QUEUE", "Already in the ranked queue",
			"You are already waiting in the ranked queue");
	}
}
