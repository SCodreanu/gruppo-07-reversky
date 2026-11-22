package it.downfall.reversky.api.collection.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.catalog.dto.CatalogCardResponse;
import it.downfall.reversky.api.catalog.dto.RaritySummaryResponse;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.collection.dto.AvailableCardResponse;
import it.downfall.reversky.api.collection.dto.CollectionEntryResponse;
import it.downfall.reversky.api.collection.dto.CollectionFilter;
import it.downfall.reversky.api.collection.dto.CollectionSummaryResponse;
import it.downfall.reversky.api.collection.dto.RarityProgressResponse;
import it.downfall.reversky.api.collection.entity.CollectionCard;
import it.downfall.reversky.api.collection.exception.InvalidCollectionSortException;
import it.downfall.reversky.api.collection.exception.NotEnoughCopiesException;
import it.downfall.reversky.api.collection.repository.CardCount;
import it.downfall.reversky.api.collection.repository.CollectionCardRepository;
import it.downfall.reversky.api.collection.repository.CollectionCardSpecifications;
import it.downfall.reversky.api.collection.repository.RarityProgressRow;
import it.downfall.reversky.api.common.dto.PageResponse;
import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.exception.UserNotFoundException;
import it.downfall.reversky.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * Collezione: le carte possedute da ogni utente (docs/collezione).
 * <p>
 * È l'unico service che scrive su {@code collection_cards}, con {@link #addCards} e {@link #removeCards}: così la
 * regola "non si perdono e non si duplicano carte" sta in un posto solo.
 */
@Service
@RequiredArgsConstructor
public class CollectionService {

	// Proprietà di "sort" ammesse e percorso nell'entità (deciso il 20 novembre); le altre sono 400
	private static final Map<String, String> SORT_PATHS = Map.of(
		"name", "card.name",
		"rarity", "card.rarity.sortingOrder",
		"type", "card.cardType",
		"quantity", "quantity");

	private final CollectionCardRepository collectionCardRepository;
	private final CatalogCardRepository catalogCardRepository;
	private final UserRepository userRepository;

	/**
	 * Aggiunge copie di carte alla collezione (apertura dei pacchetti, scambi): incrementa la riga di una carta
	 * posseduta o la crea.
	 * <p>
	 * Gira dentro la transazione del chiamante, che ha già bloccato la riga {@code inventories} dell'utente
	 * (docs/collezione): senza una transazione aperta lancia {@code IllegalTransactionStateException}.
	 *
	 * @param quantitiesByCardId copie da aggiungere per id di carta, ognuna almeno 1
	 * @throws IllegalArgumentException se la mappa è vuota o una quantità è minore di 1 (errore del chiamante)
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void addCards(Integer userId, Map<Integer, Integer> quantitiesByCardId) {
		checkQuantities(quantitiesByCardId);
		Map<Integer, CollectionCard> owned = loadRows(userId, quantitiesByCardId);

		// Ordine fisso per id: gli insert avvengono sempre nella stessa sequenza
		new TreeMap<>(quantitiesByCardId).forEach((cardId, quantity) -> {
			CollectionCard row = owned.get(cardId);
			if (row == null) {
				collectionCardRepository.save(
					new CollectionCard(userId, catalogCardRepository.getReferenceById(cardId), quantity));
			} else {
				row.add(quantity);
			}
		});
	}

	/**
	 * Toglie copie di carte dalla collezione (scambi); una riga che arriva a 0 si elimina.
	 * <p>
	 * Controlla tutte le carte prima di cambiarne una: se una non basta non cambia niente. Conta le copie possedute,
	 * non quelle disponibili (la disponibilità la controlla il chiamante). Gira dentro la transazione del chiamante,
	 * come {@link #addCards}.
	 *
	 * @param quantitiesByCardId copie da togliere per id di carta, ognuna almeno 1
	 * @throws NotEnoughCopiesException se di una carta l'utente ha meno copie di quelle da togliere
	 * @throws IllegalArgumentException se la mappa è vuota o una quantità è minore di 1 (errore del chiamante)
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void removeCards(Integer userId, Map<Integer, Integer> quantitiesByCardId) {
		checkQuantities(quantitiesByCardId);
		Map<Integer, CollectionCard> owned = loadRows(userId, quantitiesByCardId);
		Map<Integer, Integer> sorted = new TreeMap<>(quantitiesByCardId);

		sorted.forEach((cardId, quantity) -> {
			CollectionCard row = owned.get(cardId);
			int copies = row == null ? 0 : row.getQuantity();
			if (copies < quantity) {
				throw new NotEnoughCopiesException(cardId, copies, quantity);
			}
		});

		sorted.forEach((cardId, quantity) -> {
			CollectionCard row = owned.get(cardId);
			row.remove(quantity);
			if (row.getQuantity() == 0) {
				collectionCardRepository.delete(row);
			}
		});
	}

	/**
	 * La mia collezione (GET /collection): ogni carta posseduta, anche disattivata o variante, con copie, copie
	 * riservate dalle mie offerte e numero di mazzi che la usano.
	 *
	 * @param pageable {@code sort} su name, rarity, type o quantity; senza, per nome
	 * @throws InvalidCollectionSortException se {@code sort} usa un'altra proprietà
	 */
	@Transactional(readOnly = true)
	public PageResponse<CollectionEntryResponse> findMine(Integer userId, CollectionFilter filter, Pageable pageable) {
		Page<CollectionCard> page = loadPage(userId, filter, pageable);
		List<Integer> cardIds = cardIds(page);
		Map<Integer, Integer> reserved = countReserved(userId, cardIds);
		Map<Integer, Integer> decks = cardIds.isEmpty() ? Map.of()
			: toMap(collectionCardRepository.countDecksUsing(userId, cardIds));

		return PageResponse.from(page.map(row -> new CollectionEntryResponse(
			CatalogCardResponse.from(row.getCard()),
			row.getQuantity(),
			reserved.getOrDefault(row.getCardId(), 0),
			decks.getOrDefault(row.getCardId(), 0))));
	}

	/**
	 * La collezione di un altro utente (GET /users/{id}/collection), per preparare uno scambio: ogni carta con le copie
	 * disponibili, cioè possedute meno quelle riservate dalle sue offerte aperte, anche 0.
	 *
	 * @throws UserNotFoundException          se l'utente non esiste o è disabilitato
	 * @throws InvalidCollectionSortException se {@code sort} usa una proprietà non ammessa
	 */
	@Transactional(readOnly = true)
	public PageResponse<AvailableCardResponse> findOf(Integer ownerId, CollectionFilter filter, Pageable pageable) {
		userRepository.findById(ownerId)
			.filter(User::isEnabled)
			.orElseThrow(UserNotFoundException::new);

		Page<CollectionCard> page = loadPage(ownerId, filter, pageable);
		Map<Integer, Integer> reserved = countReserved(ownerId, cardIds(page));

		return PageResponse.from(page.map(row -> new AvailableCardResponse(
			CatalogCardResponse.from(row.getCard()),
			Math.max(0, row.getQuantity() - reserved.getOrDefault(row.getCardId(), 0)))));
	}

	/**
	 * Completamento della collezione (GET /collection/summary): carte originali attive possedute sul totale, in tutto e
	 * per rarità. Una variante posseduta vale come la sua originale (Q-08).
	 */
	@Transactional(readOnly = true)
	public CollectionSummaryResponse summary(Integer userId) {
		List<RarityProgressResponse> rarities = collectionCardRepository.summarize(userId).stream()
			.map(CollectionService::toProgress)
			.toList();

		int owned = rarities.stream().mapToInt(RarityProgressResponse::owned).sum();
		int total = rarities.stream().mapToInt(RarityProgressResponse::total).sum();
		return new CollectionSummaryResponse(owned, total, rarities);
	}

	/**
	 * Copie disponibili di quelle carte per l'utente: possedute meno quelle riservate dalle sue offerte di scambio
	 * aperte (docs/scambi, "Copie riservate"), mai sotto zero. Le usano i mazzi per il controllo {@code DECK_NOT_OWNED}.
	 *
	 * @return copie disponibili per id di carta; una carta non posseduta manca dalla mappa (cioè 0)
	 */
	@Transactional(readOnly = true)
	public Map<Integer, Integer> getAvailableCopies(Integer userId, Collection<Integer> cardIds) {
		if (cardIds.isEmpty()) {
			return Map.of();
		}

		List<Integer> ids = List.copyOf(cardIds);
		Map<Integer, Integer> reserved = countReserved(userId, ids);
		return collectionCardRepository.findByIdUserIdAndIdCardIdIn(userId, ids).stream()
			.collect(Collectors.toMap(CollectionCard::getCardId,
				row -> Math.max(0, row.getQuantity() - reserved.getOrDefault(row.getCardId(), 0))));
	}

	/**
	 * Copie possedute di quelle carte, senza togliere quelle riservate (docs/scambi, "Proposta"): per controllare che
	 * l'altro partecipante possieda le carte richieste senza rivelare le sue offerte aperte.
	 *
	 * @return copie per id di carta; una carta non posseduta manca dalla mappa (cioè 0)
	 */
	@Transactional(readOnly = true)
	public Map<Integer, Integer> getOwnedCopies(Integer userId, Collection<Integer> cardIds) {
		if (cardIds.isEmpty()) {
			return Map.of();
		}
		return collectionCardRepository.findByIdUserIdAndIdCardIdIn(userId, cardIds).stream()
			.collect(Collectors.toMap(CollectionCard::getCardId, CollectionCard::getQuantity));
	}

	/**
	 * Quali di quelle carte l'utente possiede già (per {@code isNew} dell'apertura dei pacchetti).
	 */
	@Transactional(readOnly = true)
	public Set<Integer> getOwnedCardIds(Integer userId, Collection<Integer> cardIds) {
		if (cardIds.isEmpty()) {
			return Set.of();
		}
		return collectionCardRepository.findByIdUserIdAndIdCardIdIn(userId, cardIds).stream()
			.map(CollectionCard::getCardId)
			.collect(Collectors.toSet());
	}

	private Page<CollectionCard> loadPage(Integer userId, CollectionFilter filter, Pageable pageable) {
		Specification<CollectionCard> specification = Specification.allOf(
			CollectionCardSpecifications.ofUser(userId),
			CollectionCardSpecifications.hasType(filter.type()),
			CollectionCardSpecifications.nameContains(filter.name()),
			CollectionCardSpecifications.hasRarity(filter.rarityId()),
			CollectionCardSpecifications.hasFaction(filter.factionId()),
			CollectionCardSpecifications.hasTag(filter.tag()));

		return collectionCardRepository.findAll(specification, withCollectionOrder(pageable));
	}

	// Ordine del client tradotto nei percorsi dell'entità, poi nome e id della carta come spareggio: così due righe
	// uguali per il criterio scelto hanno sempre lo stesso ordine tra le pagine
	private static Pageable withCollectionOrder(Pageable pageable) {
		List<Sort.Order> orders = new ArrayList<>();
		for (Sort.Order order : pageable.getSort()) {
			String path = SORT_PATHS.get(order.getProperty());
			if (path == null) {
				throw new InvalidCollectionSortException(order.getProperty());
			}
			orders.add(new Sort.Order(order.getDirection(), path));
		}

		if (orders.stream().noneMatch(order -> order.getProperty().equals("card.name"))) {
			orders.add(Sort.Order.asc("card.name"));
		}
		orders.add(Sort.Order.asc("card.id"));
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
	}

	private Map<Integer, CollectionCard> loadRows(Integer userId, Map<Integer, Integer> quantitiesByCardId) {
		return collectionCardRepository.findByIdUserIdAndIdCardIdIn(userId, quantitiesByCardId.keySet()).stream()
			.collect(Collectors.toMap(CollectionCard::getCardId, Function.identity()));
	}

	private Map<Integer, Integer> countReserved(Integer userId, List<Integer> cardIds) {
		return cardIds.isEmpty() ? Map.of() : toMap(collectionCardRepository.countReserved(userId, cardIds));
	}

	private static List<Integer> cardIds(Page<CollectionCard> page) {
		return page.getContent().stream().map(CollectionCard::getCardId).toList();
	}

	private static Map<Integer, Integer> toMap(List<CardCount> counts) {
		return counts.stream().collect(Collectors.toMap(CardCount::getCardId, count -> count.getTotal().intValue()));
	}

	private static RarityProgressResponse toProgress(RarityProgressRow row) {
		return new RarityProgressResponse(new RaritySummaryResponse(row.getRarityId(), row.getRarityName()),
			row.getOwned().intValue(), row.getTotal().intValue());
	}

	private static void checkQuantities(Map<Integer, Integer> quantitiesByCardId) {
		if (quantitiesByCardId.isEmpty()) {
			throw new IllegalArgumentException("No cards to add or remove");
		}

		quantitiesByCardId.forEach((cardId, quantity) -> {
			if (quantity == null || quantity < 1) {
				throw new IllegalArgumentException("Invalid quantity " + quantity + " for card " + cardId);
			}
		});
	}
}
