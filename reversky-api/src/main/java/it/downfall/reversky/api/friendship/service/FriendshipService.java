package it.downfall.reversky.api.friendship.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.battle.service.PendingInviteService;
import it.downfall.reversky.api.friendship.dto.FriendRequestResponse;
import it.downfall.reversky.api.friendship.dto.FriendResponse;
import it.downfall.reversky.api.friendship.entity.Friendship;
import it.downfall.reversky.api.friendship.entity.FriendshipId;
import it.downfall.reversky.api.friendship.entity.FriendshipStatus;
import it.downfall.reversky.api.friendship.exception.FriendshipAlreadyExistsException;
import it.downfall.reversky.api.friendship.exception.FriendshipNotFoundException;
import it.downfall.reversky.api.friendship.exception.FriendshipRequestNotFoundException;
import it.downfall.reversky.api.friendship.exception.FriendshipSelfRequestException;
import it.downfall.reversky.api.friendship.repository.FriendshipRepository;
import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.service.UserService;
import lombok.RequiredArgsConstructor;

/**
 * Amicizie (docs/amicizie).
 * <p>
 * Tutte le operazioni che cambiano stato sono in transazione: la riga è unica per coppia
 * (vincolo {@code uq_friendships_pair} su LEAST/GREATEST), quindi due richieste opposte
 * concorrenti non possono produrre due righe.
 */
@Service
@RequiredArgsConstructor
public class FriendshipService {

	private final FriendshipRepository friendshipRepository;
	private final UserService userService;
	private final PendingInviteService pendingInviteService;

	// --- letture ---

	/** GET /friends: amici ACCEPTED dell'utente, in entrambe le direzioni. */
	// TODO(@acistar0): togliere l'N+1. Ora si fa una query per ogni amico (userService.getById).
	// Raccogliere prima gli id con otherUserId(...), poi caricare tutti gli utenti con una sola query
	// (es. un UserService.getAllById(Collection<Integer>) che usa userRepository.findAllById e
	// restituisce una Map<Integer, User>) e costruire le risposte leggendo dalla mappa.
	// Lo stesso vale per toResponse(...), usato dalle liste delle richieste: due query per riga.
	@Transactional(readOnly = true)
	public List<FriendResponse> findFriends(Integer currentUserId) {
		return friendshipRepository.findForUserWithStatus(FriendshipStatus.ACCEPTED, currentUserId)
			.stream()
			.map(f -> userService.getById(otherUserId(f, currentUserId)))
			.map(FriendResponse::from)
			.toList();
	}

	/**
	 * I due utenti sono amici (amicizia {@code ACCEPTED}, in qualunque direzione)? Serve alle sfide amichevoli.
	 */
	@Transactional(readOnly = true)
	public boolean areFriends(Integer userId, Integer otherUserId) {
		return friendshipRepository.findBetween(userId, otherUserId)
			.map(friendship -> friendship.getStatus() == FriendshipStatus.ACCEPTED)
			.orElse(false);
	}

	/** GET /friends/requests?box=incoming: PENDING dove sono il recipient. */
	@Transactional(readOnly = true)
	public List<FriendRequestResponse> findIncomingRequests(Integer currentUserId) {
		return friendshipRepository.findReceivedWithStatus(FriendshipStatus.PENDING, currentUserId)
			.stream()
			.map(this::toResponse)
			.toList();
	}

	/** GET /friends/requests?box=outgoing: PENDING dove sono il requester. */
	@Transactional(readOnly = true)
	public List<FriendRequestResponse> findOutgoingRequests(Integer currentUserId) {
		return friendshipRepository.findSentWithStatus(FriendshipStatus.PENDING, currentUserId)
			.stream()
			.map(this::toResponse)
			.toList();
	}

	// --- scritture ---

	/**
	 * POST /friends/requests: invia una richiesta di amicizia.
	 * <p>
	 * Comportamento (docs/amicizie):
	 * <ul>
	 *   <li>se esiste già un'amicizia ACCEPTED → 409;</li>
	 *   <li>se esiste una richiesta PENDING nella <b>stessa</b> direzione → 409;</li>
	 *   <li>se esiste una richiesta PENDING nella direzione <b>inversa</b> → diventa ACCEPTED (una sola riga);</li>
	 *   <li>altrimenti crea una nuova riga PENDING.</li>
	 * </ul>
	 *
	 * @throws FriendshipSelfRequestException    se lo username è quello dell'utente autenticato
	 * @throws FriendshipAlreadyExistsException se esiste già una relazione incompatibile,
	 *                                          anche per due richieste opposte concorrenti
	 */
	@Transactional
	public FriendRequestResponse sendRequest(Integer currentUserId, String targetUsername) {
		User target = userService.getByUsername(targetUsername);

		if (target.getId().equals(currentUserId)) {
			throw new FriendshipSelfRequestException();
		}

		// Relazione esistente, in qualunque direzione
		Friendship existing = friendshipRepository.findBetween(currentUserId, target.getId()).orElse(null);
		if (existing != null) {
			if (existing.getStatus() == FriendshipStatus.ACCEPTED) {
				throw new FriendshipAlreadyExistsException();
			}
			if (existing.getId().getRequesterId().equals(currentUserId)) {
				// Stessa direzione, già PENDING: la riga esiste e non c'è niente da fare
				throw new FriendshipAlreadyExistsException();
			}
			// Richiesta inversa: diventa ACCEPTED (regola 2)
			existing.accept();
			return toResponse(existing);
		}

		// Nessuna relazione: crea PENDING. Se un'altra transazione inserisce la stessa coppia nello
		// stesso momento fa da arbitro il database: la chiave primaria nella stessa direzione,
		// uq_friendships_pair in quella opposta. Friendship è Persistable, quindi saveAndFlush fa
		// sempre un INSERT (mai un merge su una riga trovata nel frattempo).
		Friendship created = new Friendship(currentUserId, target.getId());
		try {
			friendshipRepository.saveAndFlush(created);
		} catch (DataIntegrityViolationException ex) {
			throw new FriendshipAlreadyExistsException();
		}
		return toResponse(created);
	}

	/**
	 * POST /friends/requests/{requesterId}/accept: accetta una richiesta ricevuta.
	 *
	 * @throws FriendshipRequestNotFoundException se la richiesta non esiste o non è PENDING
	 */
	@Transactional
	public FriendRequestResponse accept(Integer currentUserId, Integer requesterId) {
		Friendship friendship = loadPendingRequest(requesterId, currentUserId);
		friendship.accept();
		return toResponse(friendship);
	}

	/**
	 * POST /friends/requests/{requesterId}/reject: rifiuta una richiesta ricevuta.
	 * La riga viene eliminata, quindi si può riprovare in entrambe le direzioni (docs/amicizie, regola 4).
	 *
	 * @throws FriendshipRequestNotFoundException se la richiesta non esiste o non è PENDING
	 */
	@Transactional
	public void reject(Integer currentUserId, Integer requesterId) {
		Friendship friendship = loadPendingRequest(requesterId, currentUserId);
		friendshipRepository.delete(friendship);
	}

	/**
	 * DELETE /friends/{userId}: rimuove un'amicizia o ritira una richiesta inviata.
	 * <p>
	 * Elimina la riga e anche gli inviti di sfida PENDING tra i due, in entrambe le direzioni
	 * (docs/amicizie, regola 7).
	 *
	 * @throws FriendshipSelfRequestException se userId è l'utente autenticato
	 * @throws FriendshipNotFoundException    se non esiste alcuna relazione
	 */
	@Transactional
	public void remove(Integer currentUserId, Integer targetUserId) {
		if (currentUserId.equals(targetUserId)) {
			throw new FriendshipSelfRequestException();
		}

		Friendship existing = friendshipRepository.findBetween(currentUserId, targetUserId)
			.orElseThrow(() -> new FriendshipNotFoundException());

		friendshipRepository.delete(existing);
		pendingInviteService.deletePendingBetween(currentUserId, targetUserId);
	}

	// --- supporto ---

	private Friendship loadPendingRequest(Integer requesterId, Integer currentUserId) {
		Friendship friendship = friendshipRepository.findById(new FriendshipId(requesterId, currentUserId))
			.orElseThrow(() -> new FriendshipRequestNotFoundException());

		if (friendship.getStatus() != FriendshipStatus.PENDING) {
			throw new FriendshipRequestNotFoundException();
		}
		return friendship;
	}

	private FriendRequestResponse toResponse(Friendship friendship) {
		User requester = userService.getById(friendship.getId().getRequesterId());
		User recipient = userService.getById(friendship.getId().getRecipientId());
		return FriendRequestResponse.from(friendship, requester, recipient);
	}

	private Integer otherUserId(Friendship friendship, Integer currentUserId) {
		return friendship.getId().getRequesterId().equals(currentUserId)
			? friendship.getId().getRecipientId()
			: friendship.getId().getRequesterId();
	}
}