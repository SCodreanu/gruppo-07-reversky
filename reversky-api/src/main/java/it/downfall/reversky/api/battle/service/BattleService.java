package it.downfall.reversky.api.battle.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import it.downfall.reversky.api.battle.client.EngineClient;
import it.downfall.reversky.api.battle.client.SimulationRequest;
import it.downfall.reversky.api.battle.client.SimulationResponse;
import it.downfall.reversky.api.battle.dto.BattleDetailResponse;
import it.downfall.reversky.api.battle.dto.BattleDetailResponse.BattlePlayerResponse;
import it.downfall.reversky.api.battle.dto.BattleOutcome;
import it.downfall.reversky.api.battle.dto.BattleSummaryResponse;
import it.downfall.reversky.api.battle.dto.PlayerSummaryResponse;
import it.downfall.reversky.api.battle.entity.Battle;
import it.downfall.reversky.api.battle.entity.BattleInvite;
import it.downfall.reversky.api.battle.entity.BattleMode;
import it.downfall.reversky.api.battle.entity.BattleResult;
import it.downfall.reversky.api.battle.entity.BattleStats;
import it.downfall.reversky.api.battle.entity.BattleStats.PlayerStats;
import it.downfall.reversky.api.battle.entity.DeckCardSnapshot;
import it.downfall.reversky.api.battle.exception.BattleInviteNotFoundException;
import it.downfall.reversky.api.battle.exception.BattleNotFoundException;
import it.downfall.reversky.api.battle.exception.EngineInputRejectedException;
import it.downfall.reversky.api.battle.exception.EngineUnavailableException;
import it.downfall.reversky.api.battle.exception.InviteAlreadyAcceptedException;
import it.downfall.reversky.api.battle.exception.NotFriendsException;
import it.downfall.reversky.api.battle.repository.BattleHistoryRow;
import it.downfall.reversky.api.battle.repository.BattleInviteRepository;
import it.downfall.reversky.api.battle.repository.BattleRepository;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.service.FactionBonusService;
import it.downfall.reversky.api.catalog.service.FactionService;
import it.downfall.reversky.api.common.dto.PageResponse;
import it.downfall.reversky.api.deck.entity.Deck;
import it.downfall.reversky.api.deck.exception.InvalidDeckException;
import it.downfall.reversky.api.deck.service.DeckService;
import it.downfall.reversky.api.friendship.service.FriendshipService;
import it.downfall.reversky.api.ruleset.entity.RuleSet;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Partite (docs/sfide, API-40, API-41, API-42, API-43): accettazione di un invito con simulazione, dettaglio e
 * storico.
 * <p>
 * L'accettazione è divisa in tre passi, per non tenere aperta una transazione mentre si aspetta l'engine: preparazione
 * (transazione 1), chiamata all'engine (fuori transazione), salvataggio (transazione 2). Per questo le transazioni sono
 * esplicite con {@link TransactionTemplate} invece di {@code @Transactional} sul metodo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleService {

	private static final String CHALLENGER = "CHALLENGER";
	private static final String OPPONENT = "OPPONENT";

	private final BattleRepository battleRepository;
	private final BattleInviteRepository battleInviteRepository;
	private final DeckService deckService;
	private final FriendshipService friendshipService;
	private final RuleSetService ruleSetService;
	private final FactionService factionService;
	private final FactionBonusService factionBonusService;
	private final UserService userService;
	private final EngineClient engineClient;
	private final RandomGenerator random;
	private final Clock clock;
	private final TransactionTemplate transactionTemplate;

	/**
	 * Accetta un invito scegliendo un mazzo, fa simulare la partita all'engine e la salva
	 * (POST /api/v1/battle-invites/{id}/accept).
	 *
	 * @return la partita dal punto di vista di chi accetta
	 * @throws BattleInviteNotFoundException   se l'invito non esiste o non è per me
	 * @throws InviteAlreadyAcceptedException  se l'invito è già stato accettato
	 * @throws NotFriendsException             se i due giocatori non sono più amici
	 * @throws InvalidDeckException            se uno dei due mazzi oggi non è valido: l'invito resta in attesa
	 * @throws EngineUnavailableException      se l'engine non simula: la partita resta preparata, con {@code battleId}
	 *                                         per il retry
	 * @throws EngineInputRejectedException    se l'engine rifiuta i dati della partita: resta preparata, con
	 *                                         {@code battleId}, ma il retry darebbe lo stesso errore
	 */
	public BattleDetailResponse accept(Integer userId, UUID inviteId, Integer deckId) {
		Battle battle = transactionTemplate.execute(status -> prepareFriendly(userId, inviteId, deckId));
		return simulateAndSave(battle, userId);
	}

	/**
	 * Rilancia la simulazione di una partita rimasta senza risultato (POST /api/v1/battles/{id}/retry), con lo stesso
	 * seed, le stesse fotografie dei mazzi e le stesse regole salvate alla preparazione, anche se nel frattempo il
	 * backoffice le ha cambiate. Una partita già completata si restituisce così com'è, senza chiamare l'engine.
	 *
	 * @throws BattleNotFoundException     se la partita non esiste o non l'ho giocata
	 * @throws EngineUnavailableException    se l'engine non simula ancora, con {@code battleId}
	 * @throws EngineInputRejectedException  se l'engine rifiuta i dati salvati della partita, con {@code battleId}
	 */
	public BattleDetailResponse retry(Integer userId, UUID battleId) {
		Battle battle = transactionTemplate.execute(status -> getOwnBattle(userId, battleId));
		if (battle.isCompleted()) {
			return transactionTemplate.execute(status -> toDetail(getOwnBattle(userId, battleId), userId));
		}

		log.info("Retrying battle {} for user {}", battleId, userId);
		return simulateAndSave(battle, userId);
	}

	/**
	 * Una partita che ho giocato, con mazzi e log (GET /api/v1/battles/{id}).
	 *
	 * @throws BattleNotFoundException se la partita non esiste o non l'ho giocata
	 */
	@Transactional(readOnly = true)
	public BattleDetailResponse findMine(Integer userId, UUID battleId) {
		return toDetail(getOwnBattle(userId, battleId), userId);
	}

	/**
	 * Le partite che ho giocato, dalla più recente, senza log (GET /api/v1/battles).
	 *
	 * @param mode solo partite di quella modalità; null = tutte
	 */
	@Transactional(readOnly = true)
	public PageResponse<BattleSummaryResponse> findHistory(Integer userId, BattleMode mode, Pageable pageable) {
		Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
		Page<BattleHistoryRow> rows = mode == null
			? battleRepository.findHistory(userId, page)
			: battleRepository.findHistoryByMode(userId, mode, page);

		Set<Integer> opponentIds = new HashSet<>();
		rows.forEach(row -> opponentIds.add(otherPlayer(row.challengerId(), row.opponentId(), userId)));
		Map<Integer, String> usernames = userService.getUsernames(opponentIds);

		return PageResponse.from(rows.map(row -> toSummary(row, userId, usernames)));
	}

	// --- passo 1: preparazione ---

	private Battle prepareFriendly(Integer userId, UUID inviteId, Integer deckId) {
		BattleInvite invite = battleInviteRepository.findByIdForUpdate(inviteId)
			.filter(found -> found.getOpponentId().equals(userId))
			.orElseThrow(() -> new BattleInviteNotFoundException(inviteId));
		if (!invite.isPending()) {
			throw new InviteAlreadyAcceptedException(inviteId);
		}
		if (!friendshipService.areFriends(invite.getChallengerId(), userId)) {
			throw new NotFriendsException();
		}

		// Le collezioni possono essere cambiate dopo l'invito: si validano di nuovo entrambi i mazzi
		Deck challengerDeck = deckService.getPlayableDeck(invite.getChallengerId(), invite.getChallengerDeckId());
		Deck opponentDeck = deckService.getPlayableDeck(userId, deckId);

		RuleSet ruleSet = ruleSetService.getActiveRuleSet();
		BattleStats stats = BattleStats.prepared(random.nextLong(),
			new SimulationRequest.Rules(ruleSet.getVersion(), ruleSet.getStartingLifePoints(), ruleSet.getMaxRounds()),
			factions(), factionBonuses());

		Battle battle = Battle.friendly(inviteId, invite.getChallengerId(), userId, ruleSet.getId(),
			snapshot(challengerDeck), snapshot(opponentDeck), stats, Instant.now(clock));
		battleRepository.save(battle);
		invite.accept();

		log.info("Invite {} accepted by user {}: battle {} prepared", inviteId, userId, battle.getId());
		return battle;
	}

	private List<SimulationRequest.Faction> factions() {
		return factionService.findAll().stream()
			.map(faction -> new SimulationRequest.Faction(faction.id(), faction.name()))
			.toList();
	}

	private List<SimulationRequest.FactionBonus> factionBonuses() {
		return factionBonusService.findAllSummaries().stream()
			.map(bonus -> new SimulationRequest.FactionBonus(bonus.originFactionId(), bonus.targetFactionId(),
				bonus.atkBonus(), bonus.defBonus()))
			.toList();
	}

	// La fotografia del mazzo, in ordine di id della carta, con i valori del catalogo di adesso
	private static List<DeckCardSnapshot> snapshot(Deck deck) {
		return deck.getContent().entrySet().stream()
			.sorted(Map.Entry.comparingByKey(Comparator.comparing(CatalogCard::getId)))
			.map(entry -> {
				CatalogCard card = entry.getKey();
				return new DeckCardSnapshot(card.getId(), card.getName(), card.getCardType().name(),
					card.getRarity().getName(), card.getRarity().getSortingOrder(),
					card.getFaction() == null ? null : card.getFaction().getId(),
					card.getSecondaryFaction() == null ? null : card.getSecondaryFaction().getId(),
					card.getAtk(), card.getDef(), card.getCardSubtype() == null ? null : card.getCardSubtype().name(),
					card.getEffectCode(), entry.getValue());
			})
			.toList();
	}

	// --- passo 2: engine, fuori transazione ---

	private BattleDetailResponse simulateAndSave(Battle battle, Integer viewerId) {
		SimulationResponse response;
		try {
			response = engineClient.simulate(toRequest(battle));
		} catch (EngineUnavailableException e) {
			log.warn("Battle {} prepared but not simulated: the engine is unavailable", battle.getId());
			throw e.withBattleId(battle.getId());
		} catch (EngineInputRejectedException e) {
			// API-71: dati che l'engine non accetta; il retry riceverebbe lo stesso rifiuto
			log.error("Battle {} prepared but rejected by the engine", battle.getId());
			throw e.withBattleId(battle.getId());
		}
		return transactionTemplate.execute(status -> save(battle.getId(), response, viewerId));
	}

	// La richiesta si costruisce solo da ciò che è salvato nella partita: il retry manda esattamente la stessa partita
	static SimulationRequest toRequest(Battle battle) {
		BattleStats stats = battle.getBattleStats();
		return new SimulationRequest(stats.seed(), stats.rules(), stats.factions(), stats.factionBonuses(),
			new SimulationRequest.Player(battle.getChallengerId(), expand(battle.getChallengerDeck())),
			new SimulationRequest.Player(battle.getOpponentId(), expand(battle.getOpponentDeck())));
	}

	private static List<SimulationRequest.Card> expand(List<DeckCardSnapshot> deck) {
		List<SimulationRequest.Card> cards = new ArrayList<>();
		for (DeckCardSnapshot card : deck) {
			for (int copy = 0; copy < card.quantity(); copy++) {
				cards.add(new SimulationRequest.Card(card.cardId(), card.name(), card.type(), card.rarity(),
					card.rarityOrder(), card.factionId(), card.secondaryFactionId(), card.atk(), card.def(), card.subtype(),
					card.effectCode()));
			}
		}
		return cards;
	}

	// --- passo 3: salvataggio ---

	// Blocca la partita: se un'altra simulazione l'ha già completata non scrive niente, il risultato sarebbe identico
	private BattleDetailResponse save(UUID battleId, SimulationResponse response, Integer viewerId) {
		Battle battle = battleRepository.findByIdForUpdate(battleId)
			.orElseThrow(() -> new BattleNotFoundException(battleId));
		if (!battle.isCompleted()) {
			BattleStats stats = battle.getBattleStats().completed(response.endReason(), response.totalRounds(),
				new PlayerStats(response.challenger().finalLifePoints(), response.challenger().creaturesDefeated()),
				new PlayerStats(response.opponent().finalLifePoints(), response.opponent().creaturesDefeated()));
			battle.complete(BattleResult.valueOf(response.result()), stats, response.log());
			log.info("Battle {} completed: {}", battleId, response.result());
		}
		return toDetail(battle, viewerId);
	}

	// --- lettura ---

	private Battle getOwnBattle(Integer userId, UUID battleId) {
		return battleRepository.findById(battleId)
			.filter(battle -> battle.involves(userId))
			.orElseThrow(() -> new BattleNotFoundException(battleId));
	}

	private BattleDetailResponse toDetail(Battle battle, Integer viewerId) {
		boolean asChallenger = battle.getChallengerId().equals(viewerId);
		Map<Integer, String> usernames = userService.getUsernames(Set.of(battle.getChallengerId(), battle.getOpponentId()));
		BattleStats stats = battle.getBattleStats();

		BattlePlayerResponse challenger = new BattlePlayerResponse(battle.getChallengerId(),
			usernames.get(battle.getChallengerId()), lifePoints(stats.challenger()), defeated(stats.challenger()),
			battle.getChallengerDeck());
		BattlePlayerResponse opponent = new BattlePlayerResponse(battle.getOpponentId(),
			usernames.get(battle.getOpponentId()), lifePoints(stats.opponent()), defeated(stats.opponent()),
			battle.getOpponentDeck());

		return new BattleDetailResponse(battle.getId(), battle.getMode(), battle.getCreatedAt(), battle.isCompleted(),
			battle.getResult(), BattleOutcome.of(battle.getResult(), asChallenger), asChallenger ? CHALLENGER : OPPONENT,
			stats.endReason(), stats.totalRounds(), stats.rules().rulesVersion(), stats.seed(),
			asChallenger ? challenger : opponent, asChallenger ? opponent : challenger, battle.getRoundsLog());
	}

	private static BattleSummaryResponse toSummary(BattleHistoryRow row, Integer viewerId, Map<Integer, String> usernames) {
		boolean asChallenger = row.challengerId().equals(viewerId);
		Integer otherId = otherPlayer(row.challengerId(), row.opponentId(), viewerId);
		BattleStats stats = row.battleStats();
		PlayerStats mine = asChallenger ? stats.challenger() : stats.opponent();
		PlayerStats theirs = asChallenger ? stats.opponent() : stats.challenger();

		return new BattleSummaryResponse(row.id(), row.createdAt(), row.mode(),
			new PlayerSummaryResponse(otherId, usernames.get(otherId)), row.result() != null,
			BattleOutcome.of(row.result(), asChallenger), stats.endReason(), lifePoints(mine), lifePoints(theirs),
			stats.totalRounds(), asChallenger ? row.challengerRatingDelta() : row.opponentRatingDelta());
	}

	private static Integer otherPlayer(Integer challengerId, Integer opponentId, Integer viewerId) {
		return challengerId.equals(viewerId) ? opponentId : challengerId;
	}

	private static Integer lifePoints(PlayerStats stats) {
		return stats == null ? null : stats.finalLifePoints();
	}

	private static Integer defeated(PlayerStats stats) {
		return stats == null ? null : stats.creaturesDefeated();
	}
}
