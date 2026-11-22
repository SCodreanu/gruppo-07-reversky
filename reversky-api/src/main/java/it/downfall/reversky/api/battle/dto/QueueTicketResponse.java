package it.downfall.reversky.api.battle.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.battle.entity.MatchmakingTicket;

/**
 * Il mio ticket nella coda classificata (POST e GET /api/v1/matchmaking/queue, docs/sfide). Il client lo legge ogni
 * pochi secondi: quando diventa {@code 404} il ticket non esiste più e la partita, se c'è, è l'ultima classificata con
 * {@code createdAt} dopo {@code queuedAt}.
 *
 * @param status   sempre {@code WAITING}: finché il ticket esiste l'utente aspetta
 * @param deckId   mazzo in coda
 * @param rating   rating al momento dell'ingresso
 * @param queuedAt momento dell'ingresso
 * @param window   finestra di ricerca adesso, in punti di rating (±), tra 100 e 400
 */
public record QueueTicketResponse(

	@Schema(example = "WAITING")
	Status status,

	@Schema(example = "3")
	Integer deckId,

	@Schema(example = "1013")
	int rating,

	@Schema(example = "2026-11-22T10:20:00Z")
	Instant queuedAt,

	@Schema(example = "250")
	int window
) {

	/** Stato del ticket: per ora uno solo, perché un ticket abbinato non esiste più. */
	public enum Status {
		WAITING
	}

	/**
	 * @param now adesso, per la finestra di ricerca
	 */
	public static QueueTicketResponse from(MatchmakingTicket ticket, Instant now) {
		return new QueueTicketResponse(Status.WAITING, ticket.getDeckId(), ticket.getRating(), ticket.getQueuedAt(),
			ticket.searchWindow(now));
	}
}
