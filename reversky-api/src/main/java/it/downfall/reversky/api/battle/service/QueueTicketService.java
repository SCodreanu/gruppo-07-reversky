package it.downfall.reversky.api.battle.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.battle.repository.MatchmakingTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Toglie i ticket della coda classificata per le altre funzionalità: mazzi e backoffice degli utenti (docs/sfide,
 * docs/mazzi, docs/backoffice).
 * <p>
 * Dipende <b>solo</b> da {@link MatchmakingTicketRepository}, di proposito, come {@link PendingInviteService}:
 * {@code MatchmakingQueueService} usa {@code DeckService}, quindi {@code DeckService} non può usare quello senza un
 * ciclo di bean. Sostituisce la query nativa su {@code matchmaking_queue} scritta in {@code DeckRepository} prima che
 * esistesse la coda (API-47).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueTicketService {

	private final MatchmakingTicketRepository ticketRepository;

	/**
	 * Toglie il ticket in coda con quel mazzo, dentro la transazione del chiamante: un mazzo modificato non è più
	 * quello controllato all'ingresso. Eliminare il mazzo non serve chiamarlo: lo fa il database con
	 * {@code ON DELETE CASCADE}.
	 */
	@Transactional
	public void removeForDeck(Integer deckId) {
		if (ticketRepository.deleteByDeckId(deckId) > 0) {
			log.info("Ranked queue ticket removed: deck {} changed", deckId);
		}
	}

	/**
	 * Toglie il ticket dell'utente, se c'è, dentro la transazione del chiamante: quando esce dalla coda o quando un
	 * admin disattiva il suo account.
	 *
	 * @return {@code true} se l'utente era in coda
	 */
	@Transactional
	public boolean removeForUser(Integer userId) {
		return ticketRepository.deleteByUserId(userId) > 0;
	}
}
