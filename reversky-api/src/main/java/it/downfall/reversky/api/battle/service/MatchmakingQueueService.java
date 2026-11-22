package it.downfall.reversky.api.battle.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.battle.dto.QueueJoinRequest;
import it.downfall.reversky.api.battle.dto.QueueTicketResponse;
import it.downfall.reversky.api.battle.exception.AlreadyInQueueException;
import it.downfall.reversky.api.battle.exception.QueueTicketNotFoundException;
import it.downfall.reversky.api.battle.repository.MatchmakingTicketRepository;
import it.downfall.reversky.api.deck.exception.DeckNotFoundException;
import it.downfall.reversky.api.deck.exception.InvalidDeckException;
import it.downfall.reversky.api.deck.service.DeckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Coda classificata (docs/sfide, "Classificata: matchmaking"): entrare, leggere il proprio ticket, uscire.
 * L'abbinamento dei ticket e la partita classificata non sono qui: arrivano con API-48.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingQueueService {

	private final MatchmakingTicketRepository ticketRepository;
	private final DeckService deckService;
	private final RankingService rankingService;
	private final QueueTicketService queueTicketService;
	private final Clock clock;

	/**
	 * Entra in coda con un mazzo (POST /api/v1/matchmaking/queue). Il mazzo dev'essere mio e valido adesso; al primo
	 * ingresso si crea la riga di {@code player_ratings} con il rating iniziale, e il ticket salva il rating attuale.
	 *
	 * @throws DeckNotFoundException   se il mazzo non esiste o è di un altro utente
	 * @throws InvalidDeckException    se il mazzo oggi non rispetta le regole
	 * @throws AlreadyInQueueException se sono già in coda, anche con un altro mazzo
	 */
	@Transactional
	public QueueTicketResponse join(Integer userId, QueueJoinRequest request) {
		deckService.getPlayableDeck(userId, request.deckId());
		rankingService.createIfMissing(userId);
		int rating = rankingService.getMine(userId).rating();
		Instant now = clock.instant();
		if (ticketRepository.insertIfAbsent(userId, request.deckId(), rating, now) == 0) {
			throw new AlreadyInQueueException();
		}

		log.info("User {} joined the ranked queue with deck {} at rating {}", userId, request.deckId(), rating);
		return QueueTicketResponse.from(ticketRepository.findById(userId).orElseThrow(), now);
	}

	/**
	 * Il mio ticket, con la finestra di ricerca di adesso (GET /api/v1/matchmaking/queue).
	 *
	 * @throws QueueTicketNotFoundException se non sono in coda
	 */
	@Transactional(readOnly = true)
	public QueueTicketResponse getMine(Integer userId) {
		return ticketRepository.findById(userId)
			.map(ticket -> QueueTicketResponse.from(ticket, clock.instant()))
			.orElseThrow(QueueTicketNotFoundException::new);
	}

	/**
	 * Esce dalla coda (DELETE /api/v1/matchmaking/queue). Se non ero in coda non succede niente: il ticket può essere
	 * appena stato abbinato o tolto, e l'esito per chi esce è lo stesso.
	 */
	@Transactional
	public void leave(Integer userId) {
		if (queueTicketService.removeForUser(userId)) {
			log.info("User {} left the ranked queue", userId);
		}
	}
}
