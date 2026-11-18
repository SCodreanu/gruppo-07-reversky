package it.downfall.reversky.api.friendship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.friendship.dto.FriendRequestResponse;
import it.downfall.reversky.api.friendship.dto.FriendResponse;
import it.downfall.reversky.api.friendship.entity.Friendship;
import it.downfall.reversky.api.friendship.entity.FriendshipStatus;
import it.downfall.reversky.api.friendship.exception.FriendshipAlreadyExistsException;
import it.downfall.reversky.api.friendship.exception.FriendshipNotFoundException;
import it.downfall.reversky.api.friendship.exception.FriendshipRequestNotFoundException;
import it.downfall.reversky.api.friendship.exception.FriendshipSelfRequestException;
import it.downfall.reversky.api.friendship.repository.FriendshipRepository;
import it.downfall.reversky.api.support.TestFriends;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * {@link FriendshipService} su un PostgreSQL vero (docs/amicizie).
 * <p>
 * Le amicizie non si "resettano" tra un test e l'altro, ma ogni test crea utenti nuovi
 * ({@link TestUsers}), quindi le relazioni restano isolate.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class, TestFriends.class})
@SpringBootTest
class FriendshipServiceIT {

	@Autowired private FriendshipService friendshipService;
	@Autowired private FriendshipRepository friendshipRepository;
	@Autowired private TestUsers testUsers;
	@Autowired private TestFriends testFriends;

	@MockitoBean private EmailSender emailSender;

	// --- send ---

	@Test
	@DisplayName("A friend request creates a PENDING row between the two users")
	void sendRequest_createsPendingRow() {
		User a = testUsers.create();
		User b = testUsers.create();

		FriendRequestResponse response = friendshipService.sendRequest(a.getId(), b.getUsername());

		assertThat(response.requesterId()).isEqualTo(a.getId());
		assertThat(response.recipientId()).isEqualTo(b.getId());
		assertThat(response.status()).isEqualTo("PENDING");

		Friendship row = friendshipRepository.findBetween(a.getId(), b.getId()).orElseThrow();
		assertThat(row.getStatus()).isEqualTo(FriendshipStatus.PENDING);
	}

	@Test
	@DisplayName("A request to yourself is rejected")
	void sendRequest_toSelf_throwsSelfRequest() {
		User a = testUsers.create();

		assertThatThrownBy(() -> friendshipService.sendRequest(a.getId(), a.getUsername()))
			.isInstanceOf(FriendshipSelfRequestException.class);
	}

	@Test
	@DisplayName("A second request in the same direction is rejected (row already exists)")
	void sendRequest_sameDirection_throwsAlreadyExists() {
		User a = testUsers.create();
		User b = testUsers.create();
		friendshipService.sendRequest(a.getId(), b.getUsername());

		assertThatThrownBy(() -> friendshipService.sendRequest(a.getId(), b.getUsername()))
			.isInstanceOf(FriendshipAlreadyExistsException.class);
	}

	@Test
	@DisplayName("An opposite request against a PENDING one becomes ACCEPTED, with a single row")
	void sendRequest_oppositePending_autoAccepts() {
		User a = testUsers.create();
		User b = testUsers.create();
		friendshipService.sendRequest(a.getId(), b.getUsername());

		FriendRequestResponse response = friendshipService.sendRequest(b.getId(), a.getUsername());

		assertThat(response.status()).isEqualTo("ACCEPTED");
		// Una sola riga in totale tra i due
		assertThat(friendshipRepository.findBetween(a.getId(), b.getId())).isPresent();
		List<Friendship> accepted = friendshipRepository.findForUserWithStatus(FriendshipStatus.ACCEPTED, a.getId());
		assertThat(accepted).hasSize(1);
		assertThat(accepted.get(0).getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
	}

	@Test
	@DisplayName("A request when already friends is rejected in both directions")
	void sendRequest_alreadyFriends_throwsAlreadyExists() {
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.makeFriends(a, b);

		assertThatThrownBy(() -> friendshipService.sendRequest(a.getId(), b.getUsername()))
			.isInstanceOf(FriendshipAlreadyExistsException.class);
		assertThatThrownBy(() -> friendshipService.sendRequest(b.getId(), a.getUsername()))
			.isInstanceOf(FriendshipAlreadyExistsException.class);
	}

	@Test
	@DisplayName("Two opposite requests at the same time produce a single row")
	void sendRequest_twoOppositeConcurrent_onlyOneRow() throws Exception {
		User a = testUsers.create();
		User b = testUsers.create();
		CountDownLatch start = new CountDownLatch(1);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<?> t1 = executor.submit(() -> {
				awaitQuietly(start);
				trySend(a.getId(), b.getUsername());
				return null;
			});
			Future<?> t2 = executor.submit(() -> {
				awaitQuietly(start);
				trySend(b.getId(), a.getUsername());
				return null;
			});
			start.countDown();
			t1.get(10, TimeUnit.SECONDS);
			t2.get(10, TimeUnit.SECONDS);
		} finally {
			executor.shutdownNow();
		}

		// L'esito può essere una PENDING o una ACCEPTED (in base a chi vince la corsa),
		// ma la riga tra A e B è sempre una sola.
		assertThat(friendshipRepository.findBetween(a.getId(), b.getId())).isPresent();
		long totalBetween = friendshipRepository.findForUserWithStatus(FriendshipStatus.ACCEPTED, a.getId()).stream()
			.filter(f -> otherId(f, a.getId()).equals(b.getId()))
			.count();
		assertThat(totalBetween).isLessThanOrEqualTo(1);
	}

	// --- accept / reject ---

	@Test
	@DisplayName("The recipient can accept: status becomes ACCEPTED and the row stays")
	void accept_recipient_marksAccepted() {
		User a = testUsers.create();
		User b = testUsers.create();
		friendshipService.sendRequest(a.getId(), b.getUsername());

		FriendRequestResponse response = friendshipService.accept(b.getId(), a.getId());

		assertThat(response.status()).isEqualTo("ACCEPTED");
		assertThat(friendshipRepository.findBetween(a.getId(), b.getId()).orElseThrow().getStatus())
			.isEqualTo(FriendshipStatus.ACCEPTED);
	}

	@Test
	@DisplayName("Only the recipient can accept: another user gets 404 (row not found)")
	void accept_nonRecipient_throwsNotFound() {
		User a = testUsers.create();
		User b = testUsers.create();
		User c = testUsers.create();
		friendshipService.sendRequest(a.getId(), b.getUsername());

		assertThatThrownBy(() -> friendshipService.accept(c.getId(), a.getId()))
			.isInstanceOf(FriendshipRequestNotFoundException.class);
	}

	@Test
	@DisplayName("The recipient can reject: the row is deleted and a new request can be sent both ways")
	void reject_recipient_deletesRowAndAllowsRetry() {
		User a = testUsers.create();
		User b = testUsers.create();
		friendshipService.sendRequest(a.getId(), b.getUsername());

		friendshipService.reject(b.getId(), a.getId());
		assertThat(friendshipRepository.findBetween(a.getId(), b.getId())).isEmpty();

		// Nuova richiesta, in entrambe le direzioni
		friendshipService.sendRequest(a.getId(), b.getUsername());
		assertThat(friendshipRepository.findBetween(a.getId(), b.getId())).isPresent();
	}

	@Test
	@DisplayName("Only the recipient can reject: another user gets 404")
	void reject_nonRecipient_throwsNotFound() {
		User a = testUsers.create();
		User b = testUsers.create();
		User c = testUsers.create();
		friendshipService.sendRequest(a.getId(), b.getUsername());

		assertThatThrownBy(() -> friendshipService.reject(c.getId(), a.getId()))
			.isInstanceOf(FriendshipRequestNotFoundException.class);
	}

	// --- find ---

	@Test
	@DisplayName("The friends list contains accepted friends only, not pending requests")
	void findFriends_onlyAccepted() {
		User a = testUsers.create();
		User b = testUsers.create();
		User c = testUsers.create();
		testFriends.makeFriends(a, b);
		friendshipService.sendRequest(a.getId(), c.getUsername()); // PENDING, non conta

		List<FriendResponse> friends = friendshipService.findFriends(a.getId());

		assertThat(friends).extracting(FriendResponse::userId).containsExactly(b.getId());
	}

	@Test
	@DisplayName("Incoming requests are PENDING rows where I am the recipient")
	void findIncoming_onlyPendingAsRecipient() {
		User a = testUsers.create();
		User b = testUsers.create();
		friendshipService.sendRequest(a.getId(), b.getUsername());

		List<FriendRequestResponse> incoming = friendshipService.findIncomingRequests(b.getId());
		assertThat(incoming).hasSize(1);
		assertThat(incoming.get(0).requesterId()).isEqualTo(a.getId());

		List<FriendRequestResponse> outgoing = friendshipService.findOutgoingRequests(a.getId());
		assertThat(outgoing).hasSize(1);
		assertThat(outgoing.get(0).recipientId()).isEqualTo(b.getId());
	}

	// --- remove ---

	@Test
	@DisplayName("Removing a friend deletes the row")
	void remove_friend_deletesRow() {
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.makeFriends(a, b);

		friendshipService.remove(a.getId(), b.getId());

		assertThat(friendshipRepository.findBetween(a.getId(), b.getId())).isEmpty();
	}

	@Test
	@DisplayName("Removing a pending request works in both directions")
	void remove_pending_worksBothWays() {
		User a = testUsers.create();
		User b = testUsers.create();
		friendshipService.sendRequest(a.getId(), b.getUsername());

		friendshipService.remove(b.getId(), a.getId());

		assertThat(friendshipRepository.findBetween(a.getId(), b.getId())).isEmpty();
	}

	@Test
	@DisplayName("Removing a non-existing friendship is rejected")
	void remove_unknown_throwsNotFound() {
		User a = testUsers.create();
		User b = testUsers.create();

		assertThatThrownBy(() -> friendshipService.remove(a.getId(), b.getId()))
			.isInstanceOf(FriendshipNotFoundException.class);
	}

	@Test
	@DisplayName("Removing yourself is rejected")
	void remove_self_throwsSelfRequest() {
		User a = testUsers.create();

		assertThatThrownBy(() -> friendshipService.remove(a.getId(), a.getId()))
			.isInstanceOf(FriendshipSelfRequestException.class);
	}

	// --- supporto ---

	private static Integer otherId(Friendship f, Integer userId) {
		return f.getId().getRequesterId().equals(userId)
			? f.getId().getRecipientId()
			: f.getId().getRequesterId();
	}

	private static void awaitQuietly(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void trySend(Integer fromId, String toUsername) {
		try {
			friendshipService.sendRequest(fromId, toUsername);
		} catch (FriendshipAlreadyExistsException ignored) {
			// OK: l'altro thread ha vinto la corsa
		}
	}
}