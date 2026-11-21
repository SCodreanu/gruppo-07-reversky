package it.downfall.reversky.api.deck.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.service.CatalogCardService;
import it.downfall.reversky.api.deck.dto.DeckCardRequest;
import it.downfall.reversky.api.deck.dto.DeckRequest;
import it.downfall.reversky.api.deck.dto.DeckResponse;
import it.downfall.reversky.api.deck.dto.DeckSummaryResponse;
import it.downfall.reversky.api.deck.dto.DeckViolation;
import it.downfall.reversky.api.deck.entity.Deck;
import it.downfall.reversky.api.deck.exception.DeckInUseException;
import it.downfall.reversky.api.deck.exception.DeckNameAlreadyUsedException;
import it.downfall.reversky.api.deck.exception.DeckNotFoundException;
import it.downfall.reversky.api.deck.exception.InvalidDeckException;
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

	static final String CARD_NOT_FOUND = "DECK_CARD_NOT_FOUND";

	private final DeckRepository deckRepository;
	private final CatalogCardService catalogCardService;

	/**
	 * I miei mazzi, in ordine di nome (GET /api/v1/decks).
	 */
	@Transactional(readOnly = true)
	public List<DeckSummaryResponse> findMine(Integer userId) {
		return deckRepository.findAllByUserIdOrderByNameAscIdAsc(userId)
			.stream()
			.map(DeckSummaryResponse::from)
			.toList();
	}

	/**
	 * Un mio mazzo con le sue carte (GET /api/v1/decks/{id}).
	 *
	 * @throws DeckNotFoundException se il mazzo non esiste o è di un altro utente
	 */
	@Transactional(readOnly = true)
	public DeckResponse findMine(Integer userId, Integer deckId) {
		return DeckResponse.from(getOwnDeck(userId, deckId));
	}

	/**
	 * Crea un mazzo con le sue carte (POST /api/v1/decks).
	 *
	 * @throws DeckNameAlreadyUsedException se l'utente ha già un mazzo con quel nome
	 * @throws InvalidDeckException         se il contenuto non è valido: il mazzo non viene salvato
	 */
	@Transactional
	public DeckResponse create(Integer userId, DeckRequest request) {
		String name = request.name().trim();
		requireFreeName(userId, name, null);
		Map<CatalogCard, Integer> content = loadContent(request.cards());

		Deck deck = new Deck(userId, name);
		deck.replaceCards(content);
		deck = saveChecked(deck, name);

		log.info("Deck {} created by user {}", deck.getId(), userId);
		return DeckResponse.from(deck);
	}

	/**
	 * Sostituisce nome e contenuto del mazzo (PUT /api/v1/decks/{id}), aggiornando le righe esistenti. Se il mazzo è
	 * in coda classificata, il ticket viene rimosso.
	 *
	 * @throws DeckNotFoundException        se il mazzo non esiste o è di un altro utente
	 * @throws DeckNameAlreadyUsedException se un altro mazzo dell'utente ha già quel nome
	 * @throws InvalidDeckException         se il contenuto non è valido: il mazzo resta com'era
	 */
	@Transactional
	public DeckResponse update(Integer userId, Integer deckId, DeckRequest request) {
		Deck deck = getOwnDeck(userId, deckId);
		String name = request.name().trim();
		requireFreeName(userId, name, deckId);
		Map<CatalogCard, Integer> content = loadContent(request.cards());

		deck.rename(name);
		deck.replaceCards(content);
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

	/**
	 * Carica le carte della richiesta, nell'ordine della richiesta. Le carte che non esistono diventano violazioni
	 * {@code DECK_CARD_NOT_FOUND}: il mazzo non si salva.
	 */
	private Map<CatalogCard, Integer> loadContent(List<DeckCardRequest> cards) {
		Map<Integer, CatalogCard> byId = catalogCardService.getCardsByIds(cards.stream().map(DeckCardRequest::cardId).toList())
			.stream()
			.collect(Collectors.toMap(CatalogCard::getId, Function.identity()));

		Map<CatalogCard, Integer> content = new LinkedHashMap<>();
		List<DeckViolation> missing = new ArrayList<>();
		for (DeckCardRequest card : cards) {
			CatalogCard found = byId.get(card.cardId());
			if (found == null) {
				missing.add(new DeckViolation(CARD_NOT_FOUND, "The card " + card.cardId() + " does not exist", card.cardId()));
			} else {
				content.put(found, card.quantity());
			}
		}

		if (!missing.isEmpty()) {
			throw new InvalidDeckException(missing);
		}
		return content;
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
