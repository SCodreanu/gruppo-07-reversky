package it.downfall.reversky.api.battle.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import it.downfall.reversky.api.battle.entity.Battle;
import it.downfall.reversky.api.battle.entity.MatchmakingTicket;
import it.downfall.reversky.api.battle.repository.MatchmakingTicketRepository;
import it.downfall.reversky.api.deck.entity.Deck;
import it.downfall.reversky.api.deck.exception.DeckNotFoundException;
import it.downfall.reversky.api.deck.exception.InvalidDeckException;
import it.downfall.reversky.api.deck.service.DeckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Abbinamento della coda classificata (docs/sfide, "Abbinamento", "Anti-boosting", API-49).
 * <p>
 * Ogni ticket, dal più vecchio, si prova nella sua transazione: si blocca con {@code SKIP LOCKED}, si cerca
 * l'avversario nella sua finestra (bloccato allo stesso modo), si ricontrollano i due mazzi, si tolgono i due ticket e
 * si prepara la partita. L'engine si chiama dopo il commit, quindi nessun ticket resta bloccato mentre si aspetta la
 * simulazione. Con {@code SKIP LOCKED} due esecuzioni in parallelo non abbinano mai lo stesso ticket: quello bloccato
 * dall'altra si salta, non si aspetta.
 * <p>
 * Lo chiamano il job ogni 2 secondi e il tentativo subito dopo un ingresso in coda ({@code battle/job/MatchmakingJob}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingService {

	/** Due giocatori che hanno giocato una classificata in questo intervallo non si abbinano di nuovo. */
	static final Duration REMATCH_COOLDOWN = Duration.ofMinutes(10);

	// Lo sfidante è chi è entrato in coda per primo; a pari ora, l'id minore
	private static final Comparator<MatchmakingTicket> QUEUE_ORDER = Comparator
		.comparing(MatchmakingTicket::getQueuedAt)
		.thenComparing(MatchmakingTicket::getUserId);

	private final MatchmakingTicketRepository ticketRepository;
	private final DeckService deckService;
	private final BattleService battleService;
	private final Clock clock;
	private final TransactionTemplate transactionTemplate;

	/**
	 * Prova ad abbinare ogni ticket in coda, dal più vecchio, una partita per transazione, e fa simulare ogni partita
	 * creata. Basta un solo giro: l'abbinamento toglie soltanto ticket, quindi un ticket rimasto senza avversario non ne
	 * trova uno nuovo più avanti nello stesso giro.
	 * <p>
	 * Un errore inatteso su un ticket (es. nessun rule set attivo) si scrive nel log e annulla solo quel tentativo: si
	 * passa al ticket dopo, così un ticket difettoso non ferma la coda.
	 *
	 * @return quante partite classificate sono state create
	 */
	public int matchAll() {
		int created = 0;
		for (Integer userId : ticketRepository.findUserIdsInQueueOrder()) {
			try {
				Optional<Battle> battle = transactionTemplate.execute(status -> matchTicket(userId));
				if (battle.isPresent()) {
					created++;
					battleService.simulateRanked(battle.get());
				}
			} catch (RuntimeException e) {
				log.error("Matchmaking failed for the ranked queue ticket of user {}", userId, e);
			}
		}
		return created;
	}

	// Dentro la transazione di un solo ticket: vuoto se il ticket non c'è più, è bloccato o non trova un avversario
	private Optional<Battle> matchTicket(Integer userId) {
		Optional<MatchmakingTicket> locked = ticketRepository.lockIfFree(userId);
		if (locked.isEmpty()) {
			return Optional.empty();
		}
		MatchmakingTicket ticket = locked.get();
		Optional<Deck> deck = playableDeckOrRemove(ticket);
		if (deck.isEmpty()) {
			return Optional.empty();
		}

		Instant now = clock.instant();
		int window = ticket.searchWindow(now);
		Instant since = now.minus(REMATCH_COOLDOWN);
		// Un avversario con il mazzo non più valido perde il ticket: si cerca il prossimo
		while (true) {
			Optional<MatchmakingTicket> opponent = ticketRepository.lockOpponent(userId, ticket.getRating(),
				ticket.getRating() - window, ticket.getRating() + window, since);
			if (opponent.isEmpty()) {
				return Optional.empty();
			}
			Optional<Deck> opponentDeck = playableDeckOrRemove(opponent.get());
			if (opponentDeck.isPresent()) {
				return Optional.of(createBattle(ticket, deck.get(), opponent.get(), opponentDeck.get()));
			}
		}
	}

	// Le carte possono essere state scambiate dopo l'ingresso in coda: un mazzo non più valido toglie solo il suo ticket
	private Optional<Deck> playableDeckOrRemove(MatchmakingTicket ticket) {
		try {
			return Optional.of(deckService.getPlayableDeck(ticket.getUserId(), ticket.getDeckId()));
		} catch (InvalidDeckException | DeckNotFoundException e) {
			ticketRepository.deleteByUserId(ticket.getUserId());
			log.info("Ranked queue ticket of user {} removed: deck {} is no longer playable", ticket.getUserId(),
				ticket.getDeckId());
			return Optional.empty();
		}
	}

	private Battle createBattle(MatchmakingTicket ticket, Deck deck, MatchmakingTicket opponent, Deck opponentDeck) {
		ticketRepository.deleteByUserId(ticket.getUserId());
		ticketRepository.deleteByUserId(opponent.getUserId());
		if (QUEUE_ORDER.compare(ticket, opponent) <= 0) {
			return battleService.prepareRanked(ticket.getUserId(), deck, opponent.getUserId(), opponentDeck);
		}
		return battleService.prepareRanked(opponent.getUserId(), opponentDeck, ticket.getUserId(), deck);
	}
}
