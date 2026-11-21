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
import it.downfall.reversky.api.catalog.service.RarityService;
import it.downfall.reversky.api.deck.dto.DeckCardRequest;
import it.downfall.reversky.api.deck.dto.DeckRequest;
import it.downfall.reversky.api.deck.dto.DeckResponse;
import it.downfall.reversky.api.deck.dto.DeckRulesResponse;
import it.downfall.reversky.api.deck.dto.DeckSummaryResponse;
import it.downfall.reversky.api.deck.dto.DeckValidationRequest;
import it.downfall.reversky.api.deck.dto.DeckValidationResponse;
import it.downfall.reversky.api.deck.dto.DeckViolation;
import it.downfall.reversky.api.deck.entity.Deck;
import it.downfall.reversky.api.deck.exception.DeckInUseException;
import it.downfall.reversky.api.deck.exception.DeckNameAlreadyUsedException;
import it.downfall.reversky.api.deck.exception.DeckNotFoundException;
import it.downfall.reversky.api.deck.exception.InvalidDeckException;
import it.downfall.reversky.api.deck.repository.DeckRepository;
import it.downfall.reversky.api.ruleset.exception.NoActiveRuleSetException;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mazzi dell'utente (docs/mazzi). Ogni metodo riceve l'id dell'utente dal token e vede solo i suoi mazzi.
 * <p>
 * Le regole vengono dal rule set attivo e dalle rarità, e le controlla {@link DeckValidator}: un mazzo non valido non
 * si salva, e un mazzo salvato ricalcola {@code valid} a ogni lettura.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeckService {

	static final String CARD_NOT_FOUND = "DECK_CARD_NOT_FOUND";

	private final DeckRepository deckRepository;
	private final CatalogCardService catalogCardService;
	private final RarityService rarityService;
	private final RuleSetService ruleSetService;
	private final DeckValidator deckValidator;

	/**
	 * I miei mazzi, in ordine di nome, con {@code valid} ricalcolato (GET /api/v1/decks).
	 *
	 * @throws NoActiveRuleSetException se nessun rule set è attivo
	 */
	@Transactional(readOnly = true)
	public List<DeckSummaryResponse> findMine(Integer userId) {
		List<Deck> decks = deckRepository.findAllByUserIdOrderByNameAscIdAsc(userId);
		if (decks.isEmpty()) {
			return List.of();
		}

		DeckRules rules = activeRules();
		return decks.stream()
			.map(deck -> DeckSummaryResponse.from(deck, violationsOf(rules, deck).isEmpty()))
			.toList();
	}

	/**
	 * Un mio mazzo con le sue carte e le regole che oggi non rispetta (GET /api/v1/decks/{id}).
	 *
	 * @throws DeckNotFoundException    se il mazzo non esiste o è di un altro utente
	 * @throws NoActiveRuleSetException se nessun rule set è attivo
	 */
	@Transactional(readOnly = true)
	public DeckResponse findMine(Integer userId, Integer deckId) {
		Deck deck = getOwnDeck(userId, deckId);
		return DeckResponse.from(deck, violationsOf(activeRules(), deck));
	}

	/**
	 * Crea un mazzo con le sue carte (POST /api/v1/decks).
	 *
	 * @throws DeckNameAlreadyUsedException se l'utente ha già un mazzo con quel nome
	 * @throws InvalidDeckException         se il mazzo non è valido: non viene salvato
	 * @throws NoActiveRuleSetException     se nessun rule set è attivo
	 */
	@Transactional
	public DeckResponse create(Integer userId, DeckRequest request) {
		String name = request.name().trim();
		requireFreeName(userId, name, null);
		Map<CatalogCard, Integer> content = loadValidContent(request.cards());

		Deck deck = new Deck(userId, name);
		deck.replaceCards(content);
		deck = saveChecked(deck, name);

		log.info("Deck {} created by user {}", deck.getId(), userId);
		return DeckResponse.from(deck, List.of());
	}

	/**
	 * Sostituisce nome e contenuto del mazzo (PUT /api/v1/decks/{id}), aggiornando le righe esistenti. Se il mazzo è
	 * in coda classificata, il ticket viene rimosso.
	 *
	 * @throws DeckNotFoundException        se il mazzo non esiste o è di un altro utente
	 * @throws DeckNameAlreadyUsedException se un altro mazzo dell'utente ha già quel nome
	 * @throws InvalidDeckException         se il nuovo mazzo non è valido: resta com'era
	 * @throws NoActiveRuleSetException     se nessun rule set è attivo
	 */
	@Transactional
	public DeckResponse update(Integer userId, Integer deckId, DeckRequest request) {
		Deck deck = getOwnDeck(userId, deckId);
		String name = request.name().trim();
		requireFreeName(userId, name, deckId);
		Map<CatalogCard, Integer> content = loadValidContent(request.cards());

		deck.rename(name);
		deck.replaceCards(content);
		deckRepository.deleteQueueTicket(deckId);
		return DeckResponse.from(saveChecked(deck, name), List.of());
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

	/**
	 * Controlla un contenuto senza salvarlo (POST /api/v1/decks/validate): stesse regole del salvataggio.
	 *
	 * @throws NoActiveRuleSetException se nessun rule set è attivo
	 */
	@Transactional(readOnly = true)
	public DeckValidationResponse validate(Integer userId, DeckValidationRequest request) {
		return DeckValidationResponse.of(check(request.cards()).violations());
	}

	/**
	 * Limiti attuali del mazzo (GET /api/v1/decks/rules): composizione del rule set attivo e limiti delle rarità.
	 *
	 * @throws NoActiveRuleSetException se nessun rule set è attivo
	 */
	@Transactional(readOnly = true)
	public DeckRulesResponse rules() {
		return DeckRulesResponse.of(activeRules(), rarityService.findAll());
	}

	private Deck getOwnDeck(Integer userId, Integer deckId) {
		return deckRepository.findByIdAndUserId(deckId, userId)
			.orElseThrow(() -> new DeckNotFoundException(deckId));
	}

	private DeckRules activeRules() {
		return DeckRules.from(ruleSetService.getActiveRuleSet());
	}

	private List<DeckViolation> violationsOf(DeckRules rules, Deck deck) {
		return deckValidator.validate(rules, deck.getContent());
	}

	private Map<CatalogCard, Integer> loadValidContent(List<DeckCardRequest> cards) {
		CheckedContent checked = check(cards);
		if (!checked.violations().isEmpty()) {
			throw new InvalidDeckException(checked.violations());
		}
		return checked.cards();
	}

	/**
	 * Carica le carte della richiesta e le controlla. Le carte che non esistono diventano violazioni
	 * {@code DECK_CARD_NOT_FOUND}, in testa alla lista; le regole si controllano sulle carte trovate.
	 */
	private CheckedContent check(List<DeckCardRequest> cards) {
		Map<Integer, CatalogCard> byId = catalogCardService.getCardsByIds(cards.stream().map(DeckCardRequest::cardId).toList())
			.stream()
			.collect(Collectors.toMap(CatalogCard::getId, Function.identity()));

		Map<CatalogCard, Integer> content = new LinkedHashMap<>();
		List<DeckViolation> violations = new ArrayList<>();
		for (DeckCardRequest card : cards) {
			CatalogCard found = byId.get(card.cardId());
			if (found == null) {
				violations.add(new DeckViolation(CARD_NOT_FOUND, "The card " + card.cardId() + " does not exist", card.cardId()));
			} else {
				content.put(found, card.quantity());
			}
		}

		violations.addAll(deckValidator.validate(activeRules(), content));
		return new CheckedContent(content, violations);
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

	private record CheckedContent(Map<CatalogCard, Integer> cards, List<DeckViolation> violations) {
	}
}
