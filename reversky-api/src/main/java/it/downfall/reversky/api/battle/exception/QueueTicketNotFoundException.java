package it.downfall.reversky.api.battle.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * L'utente non è nella coda classificata: non c'è mai entrato, ne è uscito, è stato abbinato o il ticket è stato
 * tolto (mazzo modificato, account disattivato). Il client allora legge l'ultima partita (docs/sfide).
 */
public class QueueTicketNotFoundException extends BusinessException {

	public QueueTicketNotFoundException() {
		super(Category.NOT_FOUND, "QUEUE_TICKET_NOT_FOUND", "Not in the ranked queue",
			"You are not waiting in the ranked queue");
	}
}
