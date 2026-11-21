package it.downfall.reversky.api.deck.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.deck.dto.DeckRequest;
import it.downfall.reversky.api.deck.dto.DeckResponse;
import it.downfall.reversky.api.deck.entity.Deck;
import it.downfall.reversky.api.deck.exception.DeckInUseException;
import it.downfall.reversky.api.deck.exception.DeckNameAlreadyUsedException;
import it.downfall.reversky.api.deck.exception.DeckNotFoundException;
import it.downfall.reversky.api.deck.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mazzi dell'utente (docs/mazzi). Ogni metodo riceve l'id dell'utente dal token e vede solo i suoi mazzi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeckService {

	private final DeckRepository deckRepository;

	/**
	 * I miei mazzi, in ordine di nome (GET /api/v1/decks).
	 */
	@Transactional(readOnly = true)
	public List<DeckResponse> findMine(Integer userId) {
		return deckRepository.findAllByUserIdOrderByNameAscIdAsc(userId)
			.stream()
			.map(DeckResponse::from)
			.toList();
	}

	/**
	 * Un mio mazzo (GET /api/v1/decks/{id}).
	 *
	 * @throws DeckNotFoundException se il mazzo non esiste o è di un altro utente
	 */
	@Transactional(readOnly = true)
	public DeckResponse findMine(Integer userId, Integer deckId) {
		return DeckResponse.from(getOwnDeck(userId, deckId));
	}

	/**
	 * Crea un mazzo (POST /api/v1/decks).
	 *
	 * @throws DeckNameAlreadyUsedException se l'utente ha già un mazzo con quel nome
	 */
	@Transactional
	public DeckResponse create(Integer userId, DeckRequest request) {
		String name = request.name().trim();
		requireFreeName(userId, name, null);

		Deck deck = saveChecked(new Deck(userId, name), name);
		log.info("Deck {} created by user {}", deck.getId(), userId);
		return DeckResponse.from(deck);
	}

	/**
	 * Sostituisce il mazzo (PUT /api/v1/decks/{id}). Se il mazzo è in coda classificata, il ticket viene rimosso.
	 *
	 * @throws DeckNotFoundException        se il mazzo non esiste o è di un altro utente
	 * @throws DeckNameAlreadyUsedException se un altro mazzo dell'utente ha già quel nome
	 */
	@Transactional
	public DeckResponse update(Integer userId, Integer deckId, DeckRequest request) {
		Deck deck = getOwnDeck(userId, deckId);
		String name = request.name().trim();
		requireFreeName(userId, name, deckId);

		deck.rename(name);
		deckRepository.deleteQueueTicket(deckId);
		return DeckResponse.from(saveChecked(deck, name));
	}

	/**
	 * Elimina il mazzo con le sue carte (ON DELETE CASCADE) e l'eventuale ticket in coda classificata.
	 *
	 * @throws DeckNotFoundException se il mazzo non esiste o è di un altro utente
	 * @throws DeckInUseException    se il mazzo è usato da un invito di sfida ancora in attesa
	 */
	@Transactional
	public void delete(Integer userId, Integer deckId) {
		Deck deck = getOwnDeck(userId, deckId);
		if (deckRepository.isUsedByPendingInvite(deckId)) {
			throw new DeckInUseException(deckId);
		}

		deckRepository.delete(deck);
		log.info("Deck {} deleted by user {}", deckId, userId);
	}

	private Deck getOwnDeck(Integer userId, Integer deckId) {
		return deckRepository.findByIdAndUserId(deckId, userId)
			.orElseThrow(() -> new DeckNotFoundException(deckId));
	}

	private void requireFreeName(Integer userId, String name, Integer excludedId) {
		if (deckRepository.isNameTaken(userId, name, excludedId)) {
			throw new DeckNameAlreadyUsedException(name);
		}
	}

	// Due richieste contemporanee con lo stesso nome passano entrambe il controllo: la seconda trova il vincolo
	// uq_decks_user_id_name, e anche lei riceve 409 invece di 500
	private Deck saveChecked(Deck deck, String name) {
		try {
			return deckRepository.saveAndFlush(deck);
		} catch (DataIntegrityViolationException e) {
			throw new DeckNameAlreadyUsedException(name);
		}
	}
}
