package it.downfall.reversky.api.trade.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.common.dto.PageResponse;
import it.downfall.reversky.api.trade.dto.TradeResponse;
import it.downfall.reversky.api.trade.dto.TradeUserResponse;
import it.downfall.reversky.api.trade.entity.Trade;
import it.downfall.reversky.api.trade.exception.InvalidTradeBoxException;
import it.downfall.reversky.api.trade.exception.TradeClosedException;
import it.downfall.reversky.api.trade.exception.TradeNotFoundException;
import it.downfall.reversky.api.trade.repository.TradeRepository;
import it.downfall.reversky.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scambi di carte tra utenti (docs/scambi): consultazione, stati e turni.
 * <p>
 * Ogni azione su uno scambio esistente lo blocca prima ({@code SELECT ... FOR UPDATE}) e poi ne controlla stato e
 * turno: due azioni contemporanee sullo stesso scambio vanno una alla volta. Un non partecipante riceve sempre
 * {@code 404}, come uno scambio che non esiste.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

	private static final String INCOMING = "incoming";
	private static final String OUTGOING = "outgoing";

	// Ordine fisso delle liste (deciso il 22 novembre): ultima mossa prima, id per un ordine stabile tra le pagine
	private static final Sort TRADE_ORDER = Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("id"));

	private final TradeRepository tradeRepository;
	private final UserService userService;
	private final Clock clock;

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
	 * Uno scambio di cui sono partecipante, anche chiuso (GET /api/v1/trades/{id}).
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
		log.info("Trade {} rejected by user {}", tradeId, userId);
		return toResponse(trade, userId);
	}

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

	private TradeResponse toResponse(Trade trade, Integer viewerId) {
		return toResponses(List.of(trade), viewerId).get(trade.getId());
	}

	// Gli username con una query per tutta la pagina, per id dello scambio
	private Map<UUID, TradeResponse> toResponses(List<Trade> trades, Integer viewerId) {
		Map<Integer, String> usernames = userService.getUsernames(
			trades.stream().map(trade -> trade.otherThan(viewerId)).distinct().toList());
		return trades.stream().collect(Collectors.toMap(Trade::getId, trade -> {
			Integer otherId = trade.otherThan(viewerId);
			return new TradeResponse(trade.getId(), new TradeUserResponse(otherId, usernames.get(otherId)),
				trade.getStatus(), trade.isTurnOf(viewerId), trade.getCreatedAt(), trade.getUpdatedAt());
		}));
	}
}
