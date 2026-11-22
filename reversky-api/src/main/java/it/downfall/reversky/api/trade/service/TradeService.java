package it.downfall.reversky.api.trade.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.catalog.dto.CatalogCardResponse;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.service.CatalogCardService;
import it.downfall.reversky.api.collection.service.CollectionService;
import it.downfall.reversky.api.common.dto.PageResponse;
import it.downfall.reversky.api.inventory.service.InventoryService;
import it.downfall.reversky.api.notification.entity.NotificationType;
import it.downfall.reversky.api.notification.service.NotificationService;
import it.downfall.reversky.api.trade.dto.TradeCardRequest;
import it.downfall.reversky.api.trade.dto.TradeCardResponse;
import it.downfall.reversky.api.trade.dto.TradeCounterRequest;
import it.downfall.reversky.api.trade.dto.TradeRequest;
import it.downfall.reversky.api.trade.dto.TradeResponse;
import it.downfall.reversky.api.trade.dto.TradeUserResponse;
import it.downfall.reversky.api.trade.entity.Trade;
import it.downfall.reversky.api.trade.entity.TradeOffer;
import it.downfall.reversky.api.trade.exception.InvalidTradeBoxException;
import it.downfall.reversky.api.trade.exception.TradeCardNotFoundException;
import it.downfall.reversky.api.trade.exception.TradeClosedException;
import it.downfall.reversky.api.trade.exception.TradeEmptyException;
import it.downfall.reversky.api.trade.exception.TradeNotEnoughCopiesException;
import it.downfall.reversky.api.trade.exception.TradeNotFoundException;
import it.downfall.reversky.api.trade.exception.TradeNotOwnedException;
import it.downfall.reversky.api.trade.exception.TradeNotYourTurnException;
import it.downfall.reversky.api.trade.exception.TradeSameCardBothSidesException;
import it.downfall.reversky.api.trade.exception.TradeSelfException;
import it.downfall.reversky.api.trade.exception.TradeUserDisabledException;
import it.downfall.reversky.api.trade.repository.TradeOfferRepository;
import it.downfall.reversky.api.trade.repository.TradeRepository;
import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.exception.UserNotFoundException;
import it.downfall.reversky.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scambi di carte tra utenti (docs/scambi): proposta, controproposta, accettazione atomica, consultazione e rifiuto.
 * <p>
 * Ogni azione su uno scambio esistente lo blocca prima ({@code SELECT ... FOR UPDATE}) e poi ne controlla stato e
 * turno: due azioni contemporanee sullo stesso scambio vanno una alla volta. Un non partecipante riceve sempre
 * {@code 404}, come uno scambio che non esiste.
 * <p>
 * Le copie offerte restano in {@code collection_cards}: sono riservate dalla sola query di {@link CollectionService}
 * finché lo scambio è aperto, e solo per chi ha fatto l'offerta in corso. Chi propone o controproponde blocca la sua
 * riga {@code inventories} prima di contare le copie disponibili, così due proposte contemporanee sulle stesse copie
 * non passano entrambe.
 * <p>
 * Ogni mossa avvisa l'altro partecipante nella sua transazione (API-69, docs/notifiche): {@code TRADE_PROPOSED} e
 * {@code TRADE_COUNTERED} a chi deve rispondere, {@code TRADE_ACCEPTED} e {@code TRADE_REJECTED} all'altra parte.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

	private static final String INCOMING = "incoming";
	private static final String OUTGOING = "outgoing";

	// Ordine fisso delle liste (deciso il 22 novembre): ultima mossa prima, id per un ordine stabile tra le pagine
	private static final Sort TRADE_ORDER = Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("id"));

	// Le carte di un lato per nome, poi per id
	private static final Comparator<TradeCardResponse> CARD_ORDER = Comparator
		.comparing((TradeCardResponse entry) -> entry.card().name())
		.thenComparing(entry -> entry.card().id());

	private final TradeRepository tradeRepository;
	private final TradeOfferRepository tradeOfferRepository;
	private final UserService userService;
	private final CatalogCardService catalogCardService;
	private final CollectionService collectionService;
	private final InventoryService inventoryService;
	private final NotificationService notificationService;
	private final Clock clock;

	/**
	 * Propone uno scambio (POST /api/v1/trades, docs/scambi "Proposta").
	 * <p>
	 * Controlli in quest'ordine: non a se stessi, destinatario esistente e attivo, almeno una carta, nessuna carta in
	 * entrambi i lati, carte esistenti (anche disattivate), poi, con la mia riga {@code inventories} bloccata, copie
	 * disponibili di quelle che do e copie possedute dal destinatario di quelle che chiedo.
	 *
	 * @throws TradeSelfException              se il destinatario sono io
	 * @throws UserNotFoundException           se il destinatario non esiste o è disattivato
	 * @throws TradeEmptyException             se nessuno dei due lati ha carte
	 * @throws TradeSameCardBothSidesException se una carta sta in entrambi i lati
	 * @throws TradeCardNotFoundException      se una carta non esiste
	 * @throws TradeNotEnoughCopiesException   se di una carta che do ho meno copie disponibili
	 * @throws TradeNotOwnedException          se il destinatario non possiede abbastanza copie di una carta richiesta
	 */
	@Transactional
	public TradeResponse propose(Integer userId, TradeRequest request) {
		Integer recipientId = request.recipientId();
		if (userId.equals(recipientId)) {
			throw new TradeSelfException();
		}
		requireEnabled(recipientId);
		Sides sides = resolveSides(request.give(), request.receive());

		inventoryService.lockInventories(List.of(userId));
		checkAvailable(userId, byId(sides.give()));
		checkOwned(recipientId, sides.receive());

		Trade trade = new Trade(userId, recipientId, Instant.now(clock));
		trade.replaceOffers(userId, sides.give(), sides.receive());
		tradeRepository.save(trade);
		notifyOther(trade, userId, NotificationType.TRADE_PROPOSED);
		log.info("User {} proposed trade {} to user {}", userId, trade.getId(), recipientId);
		return toResponse(trade, userId);
	}

	/**
	 * Controproposta (POST /api/v1/trades/{id}/counter, docs/scambi "Controproposta"): sostituisce tutte le offerte,
	 * aggiornando le righe esistenti, e passa il turno all'altro; con il turno passa anche la riserva delle copie.
	 * <p>
	 * Dopo il blocco dello scambio: partecipante, aperto e mio turno, poi gli stessi controlli della proposta. Questo
	 * scambio non mi riserva copie, perché la riserva è di chi ha fatto l'offerta in corso, cioè l'altro.
	 *
	 * @throws TradeNotFoundException          se lo scambio non esiste o non mi riguarda
	 * @throws TradeClosedException            se lo scambio è già accettato o rifiutato
	 * @throws TradeNotYourTurnException       se tocca all'altro
	 * @throws TradeEmptyException             se nessuno dei due lati ha carte
	 * @throws TradeSameCardBothSidesException se una carta sta in entrambi i lati
	 * @throws TradeCardNotFoundException      se una carta non esiste
	 * @throws TradeNotEnoughCopiesException   se di una carta che do ho meno copie disponibili
	 * @throws TradeNotOwnedException          se l'altro non possiede abbastanza copie di una carta richiesta
	 */
	@Transactional
	public TradeResponse counter(Integer userId, UUID tradeId, TradeCounterRequest request) {
		Trade trade = lockOpenTrade(userId, tradeId);
		if (!trade.isTurnOf(userId)) {
			throw new TradeNotYourTurnException();
		}
		Sides sides = resolveSides(request.give(), request.receive());

		inventoryService.lockInventories(List.of(userId));
		checkAvailable(userId, byId(sides.give()));
		checkOwned(trade.otherThan(userId), sides.receive());

		trade.replaceOffers(userId, sides.give(), sides.receive());
		trade.passTurn(Instant.now(clock));
		notifyOther(trade, userId, NotificationType.TRADE_COUNTERED);
		log.info("User {} countered trade {}", userId, tradeId);
		return toResponse(trade, userId);
	}

	/**
	 * Accetta lo scambio e sposta le carte, tutto in una transazione (POST /api/v1/trades/{id}/accept, docs/scambi
	 * "Accettazione"): o si muovono tutte le carte, o nessuna.
	 * <p>
	 * 1. Blocca lo scambio, poi controlla partecipante, scambio aperto e mio turno, e che l'altro sia ancora attivo.
	 * 2. Blocca la riga {@code inventories} di entrambi in ordine di {@code user_id} crescente: due scambi incrociati
	 * accettati insieme non vanno in deadlock, e un pacchetto aperto nello stesso momento aspetta.
	 * 3. Controlla le mie copie disponibili di quelle che do; le carte dell'altro sono già riservate dalla sua offerta.
	 * 4. Sposta le carte con {@link CollectionService} e chiude lo scambio.
	 *
	 * @throws TradeNotFoundException        se lo scambio non esiste o non mi riguarda
	 * @throws TradeClosedException          se lo scambio è già accettato o rifiutato (anche una seconda accettazione)
	 * @throws TradeNotYourTurnException     se tocca all'altro
	 * @throws TradeUserDisabledException    se l'altro partecipante è disattivato: lo scambio resta aperto
	 * @throws TradeNotEnoughCopiesException se di una carta che do ho meno copie disponibili: lo scambio resta aperto
	 */
	@Transactional
	public TradeResponse accept(Integer userId, UUID tradeId) {
		Trade trade = lockOpenTrade(userId, tradeId);
		if (!trade.isTurnOf(userId)) {
			throw new TradeNotYourTurnException();
		}
		Integer otherId = trade.otherThan(userId);
		if (!userService.getById(otherId).isEnabled()) {
			throw new TradeUserDisabledException();
		}

		inventoryService.lockInventories(List.of(userId, otherId));
		Map<Integer, Integer> myCards = trade.offeredBy(userId);
		Map<Integer, Integer> otherCards = trade.offeredBy(otherId);
		checkAvailable(userId, myCards);

		move(userId, otherId, myCards);
		move(otherId, userId, otherCards);
		trade.accept(Instant.now(clock));
		notifyOther(trade, userId, NotificationType.TRADE_ACCEPTED);
		log.info("Trade {} accepted by user {}", tradeId, userId);
		return toResponse(trade, userId);
	}

	/**
	 * Scambi aperti in cui tocca a me ({@code incoming}) o in cui aspetto l'altro ({@code outgoing}), dall'ultima
	 * mossa (GET /api/v1/trades?box=...). Gli scambi chiusi si leggono solo con {@link #findById}.
	 *
	 * @param pageable si usano solo pagina e dimensione: l'ordine è fisso
	 * @throws InvalidTradeBoxException se box non è incoming né outgoing
	 */
	@Transactional(readOnly = true)
	public PageResponse<TradeResponse> findBox(Integer userId, String box, Pageable pageable) {
		Pageable ordered = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), TRADE_ORDER);
		Page<Trade> page = switch (box == null ? "" : box) {
			case INCOMING -> tradeRepository.findIncoming(userId, ordered);
			case OUTGOING -> tradeRepository.findOutgoing(userId, ordered);
			default -> throw new InvalidTradeBoxException(box);
		};
		Map<UUID, TradeResponse> responses = toResponses(page.getContent(), userId);
		return PageResponse.from(page.map(trade -> responses.get(trade.getId())));
	}

	/**
	 * Uno scambio di cui sono partecipante, anche chiuso, con le offerte attuali (GET /api/v1/trades/{id}).
	 *
	 * @throws TradeNotFoundException se lo scambio non esiste o non mi riguarda
	 */
	@Transactional(readOnly = true)
	public TradeResponse findById(Integer userId, UUID tradeId) {
		Trade trade = tradeRepository.findById(tradeId)
			.filter(found -> found.involves(userId))
			.orElseThrow(() -> new TradeNotFoundException(tradeId));
		return toResponse(trade, userId);
	}

	/**
	 * Rifiuta o ritira uno scambio aperto (POST /api/v1/trades/{id}/reject): lo può fare chiunque dei due, anche fuori
	 * turno. Le copie riservate tornano disponibili da sole, perché si contano solo sugli scambi aperti.
	 *
	 * @throws TradeNotFoundException se lo scambio non esiste o non mi riguarda
	 * @throws TradeClosedException   se lo scambio è già accettato o rifiutato
	 */
	@Transactional
	public TradeResponse reject(Integer userId, UUID tradeId) {
		Trade trade = lockOpenTrade(userId, tradeId);
		trade.reject(Instant.now(clock));
		notifyOther(trade, userId, NotificationType.TRADE_REJECTED);
		log.info("Trade {} rejected by user {}", tradeId, userId);
		return toResponse(trade, userId);
	}

	// --- notifiche ---

	// API-69: all'altro partecipante, con id e username di chi ha fatto la mossa, nella transazione della mossa
	private void notifyOther(Trade trade, Integer actorId, NotificationType type) {
		notificationService.notify(trade.otherThan(actorId), type, Map.of("tradeId", trade.getId(), "userId", actorId,
			"username", userService.getById(actorId).getUsername()));
	}

	// --- controlli ---

	// Blocca lo scambio, poi controlla che mi riguardi e che sia aperto, in quest'ordine
	private Trade lockOpenTrade(Integer userId, UUID tradeId) {
		Trade trade = tradeRepository.findByIdForUpdate(tradeId)
			.filter(found -> found.involves(userId))
			.orElseThrow(() -> new TradeNotFoundException(tradeId));
		if (!trade.isOpen()) {
			throw new TradeClosedException(trade.getStatus());
		}
		return trade;
	}

	// 404 e non 422 anche per un utente disattivato, come gli inviti e GET /users/{id}/collection
	private void requireEnabled(Integer userId) {
		User user = userService.getById(userId);
		if (!user.isEnabled()) {
			throw new UserNotFoundException();
		}
	}

	// Le carte dei due lati dal catalogo, dopo i controlli che non dipendono dalle collezioni
	private Sides resolveSides(List<TradeCardRequest> give, List<TradeCardRequest> receive) {
		if (give.isEmpty() && receive.isEmpty()) {
			throw new TradeEmptyException();
		}
		Set<Integer> given = give.stream().map(TradeCardRequest::cardId).collect(Collectors.toSet());
		receive.stream()
			.map(TradeCardRequest::cardId)
			.filter(given::contains)
			.findFirst()
			.ifPresent(cardId -> {
				throw new TradeSameCardBothSidesException(cardId);
			});

		Set<Integer> ids = new HashSet<>(given);
		receive.forEach(card -> ids.add(card.cardId()));
		Map<Integer, CatalogCard> cards = catalogCardService.getCardsByIds(ids).stream()
			.collect(Collectors.toMap(CatalogCard::getId, Function.identity()));
		return new Sides(toQuantities(give, cards), toQuantities(receive, cards));
	}

	private static Map<CatalogCard, Integer> toQuantities(List<TradeCardRequest> side, Map<Integer, CatalogCard> cards) {
		Map<CatalogCard, Integer> quantities = new LinkedHashMap<>();
		for (TradeCardRequest entry : side) {
			CatalogCard card = cards.get(entry.cardId());
			if (card == null) {
				throw new TradeCardNotFoundException(entry.cardId());
			}
			quantities.put(card, entry.quantity());
		}
		return quantities;
	}

	/**
	 * Le copie disponibili di chi cede bastano. Chi propone, controproponde o accetta non è mai chi ha fatto l'offerta
	 * in corso, quindi questo scambio non gli riserva niente: le sue copie qui sono già tutte nelle disponibili.
	 */
	private void checkAvailable(Integer giverId, Map<Integer, Integer> quantitiesByCardId) {
		Map<Integer, Integer> available = collectionService.getAvailableCopies(giverId, quantitiesByCardId.keySet());
		quantitiesByCardId.forEach((cardId, quantity) -> {
			int copies = available.getOrDefault(cardId, 0);
			if (copies < quantity) {
				throw new TradeNotEnoughCopiesException(cardId, copies, quantity);
			}
		});
	}

	// Un lato vuoto (regalo o richiesta) non muove niente: CollectionService non accetta mappe vuote
	private void move(Integer fromId, Integer toId, Map<Integer, Integer> quantitiesByCardId) {
		if (quantitiesByCardId.isEmpty()) {
			return;
		}
		collectionService.removeCards(fromId, quantitiesByCardId);
		collectionService.addCards(toId, quantitiesByCardId);
	}

	private static Map<Integer, Integer> byId(Map<CatalogCard, Integer> side) {
		Map<Integer, Integer> quantities = new LinkedHashMap<>();
		side.forEach((card, quantity) -> quantities.put(card.getId(), quantity));
		return quantities;
	}

	// Dell'altro si guardano solo le copie possedute, per non rivelare quelle riservate (docs/scambi)
	private void checkOwned(Integer ownerId, Map<CatalogCard, Integer> requested) {
		Map<Integer, Integer> owned = collectionService.getOwnedCopies(ownerId, ids(requested));
		requested.forEach((card, quantity) -> {
			if (owned.getOrDefault(card.getId(), 0) < quantity) {
				throw new TradeNotOwnedException(card.getId());
			}
		});
	}

	private static Set<Integer> ids(Map<CatalogCard, Integer> side) {
		return side.keySet().stream().map(CatalogCard::getId).collect(Collectors.toSet());
	}

	// --- risposte ---

	private TradeResponse toResponse(Trade trade, Integer viewerId) {
		return toResponses(List.of(trade), viewerId).get(trade.getId());
	}

	// Username e offerte con una query ciascuno per tutta la pagina, per id dello scambio
	private Map<UUID, TradeResponse> toResponses(List<Trade> trades, Integer viewerId) {
		if (trades.isEmpty()) {
			return Map.of();
		}
		Map<Integer, String> usernames = userService.getUsernames(
			trades.stream().map(trade -> trade.otherThan(viewerId)).distinct().toList());
		Map<UUID, List<TradeOffer>> offers = tradeOfferRepository
			.findAllByIdTradeIdIn(trades.stream().map(Trade::getId).toList()).stream()
			.collect(Collectors.groupingBy(offer -> offer.getId().getTradeId()));

		return trades.stream().collect(Collectors.toMap(Trade::getId, trade -> {
			Integer otherId = trade.otherThan(viewerId);
			List<TradeOffer> tradeOffers = offers.getOrDefault(trade.getId(), List.of());
			return new TradeResponse(trade.getId(), new TradeUserResponse(otherId, usernames.get(otherId)),
				trade.getStatus(), trade.isTurnOf(viewerId), side(tradeOffers, viewerId), side(tradeOffers, otherId),
				trade.getCreatedAt(), trade.getUpdatedAt());
		}));
	}

	private static List<TradeCardResponse> side(List<TradeOffer> offers, Integer giverId) {
		return offers.stream()
			.filter(offer -> offer.getUserId().equals(giverId))
			.map(offer -> new TradeCardResponse(CatalogCardResponse.from(offer.getCard()), offer.getQuantity()))
			.sorted(CARD_ORDER)
			.toList();
	}

	/** Le carte di un lato e dell'altro, dal punto di vista di chi propone o controproponde. */
	private record Sides(Map<CatalogCard, Integer> give, Map<CatalogCard, Integer> receive) {
	}
}
