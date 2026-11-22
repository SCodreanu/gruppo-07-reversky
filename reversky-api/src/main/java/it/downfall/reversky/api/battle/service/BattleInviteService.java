package it.downfall.reversky.api.battle.service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.battle.dto.BattleInviteRequest;
import it.downfall.reversky.api.battle.dto.BattleInviteResponse;
import it.downfall.reversky.api.battle.dto.BattleInviteResponse.DeckRef;
import it.downfall.reversky.api.battle.dto.PlayerSummaryResponse;
import it.downfall.reversky.api.battle.entity.BattleInvite;
import it.downfall.reversky.api.battle.entity.BattleInviteStatus;
import it.downfall.reversky.api.battle.exception.BattleInviteNotFoundException;
import it.downfall.reversky.api.battle.exception.InvalidInviteBoxException;
import it.downfall.reversky.api.battle.exception.InviteAlreadyPendingException;
import it.downfall.reversky.api.battle.exception.NotFriendsException;
import it.downfall.reversky.api.battle.exception.SelfChallengeException;
import it.downfall.reversky.api.battle.repository.BattleInviteRepository;
import it.downfall.reversky.api.deck.exception.DeckNotFoundException;
import it.downfall.reversky.api.deck.exception.InvalidDeckException;
import it.downfall.reversky.api.deck.service.DeckService;
import it.downfall.reversky.api.friendship.service.FriendshipService;
import it.downfall.reversky.api.notification.entity.NotificationType;
import it.downfall.reversky.api.notification.service.NotificationService;
import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.exception.UserNotFoundException;
import it.downfall.reversky.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Inviti alle sfide amichevoli (docs/sfide, "Amichevole: invito", API-37, API-38, API-39): creazione, liste, rifiuto e
 * ritiro. L'accettazione, che crea la partita, è in {@link BattleService}. Un invito nuovo crea la notifica
 * {@code BATTLE_INVITE_RECEIVED} per l'avversario (API-68); rifiuto e ritiro no.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleInviteService {

	private static final String INCOMING = "incoming";
	private static final String OUTGOING = "outgoing";

	private final BattleInviteRepository battleInviteRepository;
	private final UserService userService;
	private final FriendshipService friendshipService;
	private final DeckService deckService;
	private final NotificationService notificationService;
	private final Clock clock;

	/**
	 * Invita un amico a una sfida amichevole con uno dei miei mazzi (POST /api/v1/battle-invites).
	 *
	 * @throws SelfChallengeException        se l'avversario sono io
	 * @throws UserNotFoundException         se l'avversario non esiste o è disattivato
	 * @throws NotFriendsException           se l'avversario non è un amico
	 * @throws DeckNotFoundException         se il mazzo non esiste o è di un altro utente
	 * @throws InvalidDeckException          se il mazzo oggi non è valido
	 * @throws InviteAlreadyPendingException se ho già un invito in attesa verso quell'avversario
	 */
	@Transactional
	public BattleInviteResponse create(Integer userId, BattleInviteRequest request) {
		Integer opponentId = request.opponentId();
		if (userId.equals(opponentId)) {
			throw new SelfChallengeException();
		}
		requireEnabled(opponentId);
		if (!friendshipService.areFriends(userId, opponentId)) {
			throw new NotFriendsException();
		}
		deckService.getPlayableDeck(userId, request.deckId());
		if (battleInviteRepository.existsByChallengerIdAndOpponentIdAndStatus(userId, opponentId, BattleInviteStatus.PENDING)) {
			throw new InviteAlreadyPendingException();
		}

		BattleInvite invite = new BattleInvite(userId, request.deckId(), opponentId, Instant.now(clock));
		try {
			battleInviteRepository.saveAndFlush(invite);
		} catch (DataIntegrityViolationException e) {
			// Due inviti contemporanei verso lo stesso avversario: il secondo trova uq_battle_invites_pending_pair
			throw new InviteAlreadyPendingException();
		}

		// API-68: l'avversario lo sa subito, con id e username di chi lo sfida
		notificationService.notify(opponentId, NotificationType.BATTLE_INVITE_RECEIVED, Map.of("inviteId", invite.getId(),
			"userId", userId, "username", userService.getById(userId).getUsername()));

		log.info("User {} challenged user {} (invite {})", userId, opponentId, invite.getId());
		return toResponses(List.of(invite), userId).get(0);
	}

	/**
	 * Inviti in attesa ricevuti ({@code incoming}) o inviati ({@code outgoing}), dal più recente
	 * (GET /api/v1/battle-invites?box=...).
	 *
	 * @throws InvalidInviteBoxException se box non è incoming né outgoing
	 */
	@Transactional(readOnly = true)
	public List<BattleInviteResponse> findBox(Integer userId, String box) {
		List<BattleInvite> invites = switch (box == null ? "" : box) {
			case INCOMING -> battleInviteRepository.findAllByOpponentIdAndStatusOrderByCreatedAtDesc(userId, BattleInviteStatus.PENDING);
			case OUTGOING -> battleInviteRepository.findAllByChallengerIdAndStatusOrderByCreatedAtDesc(userId, BattleInviteStatus.PENDING);
			default -> throw new InvalidInviteBoxException(box);
		};
		return toResponses(invites, userId);
	}

	/**
	 * Rifiuta un invito ricevuto: la riga si elimina (POST /api/v1/battle-invites/{id}/reject).
	 *
	 * @throws BattleInviteNotFoundException se l'invito non esiste, non è per me o non è più in attesa
	 */
	@Transactional
	public void reject(Integer userId, UUID inviteId) {
		BattleInvite invite = getPendingInvite(inviteId);
		if (!invite.getOpponentId().equals(userId)) {
			throw new BattleInviteNotFoundException(inviteId);
		}
		battleInviteRepository.delete(invite);
		log.info("User {} rejected invite {}", userId, inviteId);
	}

	/**
	 * Ritira un invito inviato: la riga si elimina (DELETE /api/v1/battle-invites/{id}).
	 *
	 * @throws BattleInviteNotFoundException se l'invito non esiste, non è mio o non è più in attesa
	 */
	@Transactional
	public void withdraw(Integer userId, UUID inviteId) {
		BattleInvite invite = getPendingInvite(inviteId);
		if (!invite.getChallengerId().equals(userId)) {
			throw new BattleInviteNotFoundException(inviteId);
		}
		battleInviteRepository.delete(invite);
		log.info("User {} withdrew invite {}", userId, inviteId);
	}

	private BattleInvite getPendingInvite(UUID inviteId) {
		return battleInviteRepository.findByIdForUpdate(inviteId)
			.filter(BattleInvite::isPending)
			.orElseThrow(() -> new BattleInviteNotFoundException(inviteId));
	}

	private void requireEnabled(Integer userId) {
		User user = userService.getById(userId);
		if (!user.isEnabled()) {
			throw new UserNotFoundException();
		}
	}

	// Username e nomi dei mazzi con una query ciascuno per tutta la lista; il mazzo solo per lo sfidante
	private List<BattleInviteResponse> toResponses(List<BattleInvite> invites, Integer viewerId) {
		Set<Integer> userIds = new HashSet<>();
		Set<Integer> deckIds = new HashSet<>();
		invites.forEach(invite -> {
			userIds.add(invite.getChallengerId());
			userIds.add(invite.getOpponentId());
			if (invite.getChallengerId().equals(viewerId) && invite.getChallengerDeckId() != null) {
				deckIds.add(invite.getChallengerDeckId());
			}
		});
		Map<Integer, String> usernames = userService.getUsernames(userIds);
		Map<Integer, String> deckNames = deckIds.isEmpty() ? Map.of() : deckService.getDeckNames(deckIds);

		return invites.stream().map(invite -> {
			DeckRef deck = invite.getChallengerId().equals(viewerId) && invite.getChallengerDeckId() != null
				? new DeckRef(invite.getChallengerDeckId(), deckNames.get(invite.getChallengerDeckId()))
				: null;
			return new BattleInviteResponse(invite.getId(),
				new PlayerSummaryResponse(invite.getChallengerId(), usernames.get(invite.getChallengerId())),
				new PlayerSummaryResponse(invite.getOpponentId(), usernames.get(invite.getOpponentId())),
				deck, invite.getCreatedAt());
		}).toList();
	}
}
