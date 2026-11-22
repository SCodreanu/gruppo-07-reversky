package it.downfall.reversky.api.friendship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
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
import it.downfall.reversky.api.support.TestFriends;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.exception.UserNotFoundException;

/**
 * {@link FriendshipService} su un PostgreSQL vero (docs/amicizie, "Regole" e "Test minimi").
 * <p>
 * Le amicizie non si cancellano tra un test e l'altro, ma ogni test crea utenti nuovi
 * ({@link TestUsers}), quindi le relazioni restano isolate.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class FriendshipServiceIT {

	@Autowired
	private FriendshipService friendshipService;

	@Autowired
	private FriendshipRepository friendshipRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TestUsers testUsers;

	// Non serve al test: con la stessa configurazione degli altri IT, Spring riusa il contesto
	// già avviato (e il suo container) invece di crearne uno nuovo.
	@MockitoBean
	private EmailSender emailSender;

	private TestFriends testFriends;

	@BeforeEach
	void setUp() {
		testFriends = new TestFriends(friendshipService, jdbcTemplate);
	}

	// --- invio ---

	@Test
	@DisplayName("A friend request creates a PENDING row from the requester to the recipient")
	void sendRequest_newPair_createsPendingRow() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();

		// Act
		FriendRequestResponse response = friendshipService.sendRequest(a.getId(), b.getUsername());

		// Assert
		assertThat(response.requesterId()).isEqualTo(a.getId());
		assertThat(response.requesterUsername()).isEqualTo(a.getUsername());
		assertThat(response.recipientId()).isEqualTo(b.getId());
		assertThat(response.recipientUsername()).isEqualTo(b.getUsername());
		assertThat(response.status()).isEqualTo("PENDING");
		assertThat(response.createdAt()).isNotNull();
		Friendship row = friendshipRepository.findById(new FriendshipId(a.getId(), b.getId())).orElseThrow();
		assertThat(row.getStatus()).isEqualTo(FriendshipStatus.PENDING);
	}

	@Test
	@DisplayName("A request to yourself is rejected and nothing is saved")
	void sendRequest_toSelf_throwsSelfRequest() {
		// Arrange
		User a = testUsers.create();

		// Act + Assert
		assertThatThrownBy(() -> friendshipService.sendRequest(a.getId(), a.getUsername()))
			.isInstanceOf(FriendshipSelfRequestException.class);
		assertThat(testFriends.countRowsBetween(a, a)).isZero();
	}

	@Test
	@DisplayName("A request to an unknown username is rejected")
	void sendRequest_unknownUsername_throwsUserNotFound() {
		// Arrange
		User a = testUsers.create();

		// Act + Assert
		assertThatThrownBy(() -> friendshipService.sendRequest(a.getId(), "missing_" + TestUsers.uniqueSuffix()))
			.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	@DisplayName("Usernames are case-sensitive: a different case does not find the user")
	void sendRequest_usernameWithDifferentCase_throwsUserNotFound() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();

		// Act + Assert
		assertThatThrownBy(() -> friendshipService.sendRequest(a.getId(), b.getUsername().toUpperCase()))
			.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	@DisplayName("A second request in the same direction is rejected and the row stays PENDING")
	void sendRequest_sameDirectionTwice_throwsAlreadyExists() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act + Assert
		assertThatThrownBy(() -> friendshipService.sendRequest(a.getId(), b.getUsername()))
			.isInstanceOf(FriendshipAlreadyExistsException.class);
		assertThat(testFriends.countRowsBetween(a, b)).isEqualTo(1);
		assertThat(friendshipRepository.findBetween(a.getId(), b.getId()).orElseThrow().getStatus())
			.isEqualTo(FriendshipStatus.PENDING);
	}

	@Test
	@DisplayName("Rule 2: an opposite request to a PENDING one accepts it, with a single row")
	void sendRequest_oppositeToPending_acceptsWithSingleRow() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act
		FriendRequestResponse response = friendshipService.sendRequest(b.getId(), a.getUsername());

		// Assert: la riga resta quella di A → B, ora ACCEPTED
		assertThat(response.status()).isEqualTo("ACCEPTED");
		assertThat(response.requesterId()).isEqualTo(a.getId());
		assertThat(response.recipientId()).isEqualTo(b.getId());
		assertThat(testFriends.countRowsBetween(a, b)).isEqualTo(1);
		assertThat(friendshipRepository.findById(new FriendshipId(a.getId(), b.getId())).orElseThrow().getStatus())
			.isEqualTo(FriendshipStatus.ACCEPTED);
	}

	@Test
	@DisplayName("Rule 3: a request between friends is rejected in both directions")
	void sendRequest_alreadyFriends_throwsAlreadyExistsBothWays() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.makeFriends(a, b);

		// Act + Assert
		assertThatThrownBy(() -> friendshipService.sendRequest(a.getId(), b.getUsername()))
			.isInstanceOf(FriendshipAlreadyExistsException.class);
		assertThatThrownBy(() -> friendshipService.sendRequest(b.getId(), a.getUsername()))
			.isInstanceOf(FriendshipAlreadyExistsException.class);
		assertThat(testFriends.countRowsBetween(a, b)).isEqualTo(1);
	}

	@Test
	@DisplayName("Rule 5: two opposite requests at the same time leave a single row")
	void sendRequest_twoOppositeConcurrent_singleRow() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();

		// Act
		List<Outcome> outcomes = runConcurrently(
			() -> friendshipService.sendRequest(a.getId(), b.getUsername()),
			() -> friendshipService.sendRequest(b.getId(), a.getUsername()));

		// Assert: o la seconda riceve 409, o arriva dopo il commit della prima e la accetta;
		// in entrambi i casi la riga tra A e B è una sola
		assertThat(testFriends.countRowsBetween(a, b)).isEqualTo(1);
		long conflicts = outcomes.stream().filter(Outcome::conflict).count();
		FriendshipStatus status = friendshipRepository.findBetween(a.getId(), b.getId()).orElseThrow().getStatus();
		if (conflicts == 1) {
			assertThat(status).isEqualTo(FriendshipStatus.PENDING);
		} else {
			assertThat(conflicts).isZero();
			assertThat(status).isEqualTo(FriendshipStatus.ACCEPTED);
		}
	}

	@Test
	@DisplayName("Two identical requests at the same time: one row, the other request gets a conflict")
	void sendRequest_twoSameDirectionConcurrent_oneConflict() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();

		// Act
		List<Outcome> outcomes = runConcurrently(
			() -> friendshipService.sendRequest(a.getId(), b.getUsername()),
			() -> friendshipService.sendRequest(a.getId(), b.getUsername()));

		// Assert: la chiave primaria fa da arbitro, mai due risposte 200
		assertThat(outcomes).filteredOn(Outcome::conflict).hasSize(1);
		assertThat(testFriends.countRowsBetween(a, b)).isEqualTo(1);
	}

	// --- accettazione e rifiuto ---

	@Test
	@DisplayName("The recipient accepts: the row becomes ACCEPTED")
	void accept_byRecipient_marksAccepted() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act
		FriendRequestResponse response = friendshipService.accept(b.getId(), a.getId());

		// Assert
		assertThat(response.status()).isEqualTo("ACCEPTED");
		assertThat(friendshipRepository.findBetween(a.getId(), b.getId()).orElseThrow().getStatus())
			.isEqualTo(FriendshipStatus.ACCEPTED);
	}

	@Test
	@DisplayName("The requester cannot accept their own request")
	void accept_byRequester_throwsRequestNotFound() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act + Assert
		assertThatThrownBy(() -> friendshipService.accept(a.getId(), b.getId()))
			.isInstanceOf(FriendshipRequestNotFoundException.class);
		assertThat(friendshipRepository.findBetween(a.getId(), b.getId()).orElseThrow().getStatus())
			.isEqualTo(FriendshipStatus.PENDING);
	}

	@Test
	@DisplayName("A third user cannot accept someone else's request")
	void accept_byThirdUser_throwsRequestNotFound() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		User c = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act + Assert
		assertThatThrownBy(() -> friendshipService.accept(c.getId(), a.getId()))
			.isInstanceOf(FriendshipRequestNotFoundException.class);
	}

	@Test
	@DisplayName("An already accepted request cannot be accepted again")
	void accept_alreadyAccepted_throwsRequestNotFound() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.makeFriends(a, b);

		// Act + Assert
		assertThatThrownBy(() -> friendshipService.accept(b.getId(), a.getId()))
			.isInstanceOf(FriendshipRequestNotFoundException.class);
	}

	@Test
	@DisplayName("Rule 4: rejecting deletes the row, and the rejected user can send a new request")
	void reject_byRecipient_deletesRowAndAllowsReverseRequest() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act
		friendshipService.reject(b.getId(), a.getId());

		// Assert: riga eliminata, poi una richiesta nella direzione opposta crea una nuova PENDING
		assertThat(testFriends.countRowsBetween(a, b)).isZero();
		FriendRequestResponse again = friendshipService.sendRequest(b.getId(), a.getUsername());
		assertThat(again.status()).isEqualTo("PENDING");
		assertThat(again.requesterId()).isEqualTo(b.getId());
		assertThat(testFriends.countRowsBetween(a, b)).isEqualTo(1);
	}

	@Test
	@DisplayName("Rule 4: after a rejection the same requester can try again")
	void reject_byRecipient_allowsSameDirectionRequest() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);
		friendshipService.reject(b.getId(), a.getId());

		// Act
		FriendRequestResponse again = friendshipService.sendRequest(a.getId(), b.getUsername());

		// Assert
		assertThat(again.status()).isEqualTo("PENDING");
		assertThat(again.requesterId()).isEqualTo(a.getId());
		assertThat(testFriends.countRowsBetween(a, b)).isEqualTo(1);
	}

	@Test
	@DisplayName("Only the recipient can reject: the requester and a third user get not found")
	void reject_byNonRecipient_throwsRequestNotFound() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		User c = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act + Assert
		assertThatThrownBy(() -> friendshipService.reject(a.getId(), b.getId()))
			.isInstanceOf(FriendshipRequestNotFoundException.class);
		assertThatThrownBy(() -> friendshipService.reject(c.getId(), a.getId()))
			.isInstanceOf(FriendshipRequestNotFoundException.class);
		assertThat(testFriends.countRowsBetween(a, b)).isEqualTo(1);
	}

	@Test
	@DisplayName("A friendship cannot be rejected: reject works only on PENDING requests")
	void reject_acceptedFriendship_throwsRequestNotFound() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.makeFriends(a, b);

		// Act + Assert
		assertThatThrownBy(() -> friendshipService.reject(b.getId(), a.getId()))
			.isInstanceOf(FriendshipRequestNotFoundException.class);
		assertThat(testFriends.countRowsBetween(a, b)).isEqualTo(1);
	}

	// --- letture ---

	@Test
	@DisplayName("Rule 6: the friends list contains friends from both directions, not pending requests")
	void findFriends_bothDirections_onlyAccepted() {
		// Arrange
		User a = testUsers.create();
		User sentByA = testUsers.create();
		User sentToA = testUsers.create();
		User pending = testUsers.create();
		testFriends.makeFriends(a, sentByA);
		testFriends.makeFriends(sentToA, a);
		testFriends.sendRequest(a, pending);

		// Act
		List<FriendResponse> friends = friendshipService.findFriends(a.getId());

		// Assert
		assertThat(friends)
			.extracting(FriendResponse::userId)
			.containsExactlyInAnyOrder(sentByA.getId(), sentToA.getId());
		assertThat(friends)
			.extracting(FriendResponse::username)
			.containsExactlyInAnyOrder(sentByA.getUsername(), sentToA.getUsername());
	}

	@Test
	@DisplayName("A user with no friends gets an empty list")
	void findFriends_noFriends_returnsEmptyList() {
		// Arrange
		User a = testUsers.create();

		// Act + Assert
		assertThat(friendshipService.findFriends(a.getId())).isEmpty();
	}

	@Test
	@DisplayName("Incoming requests are the PENDING ones I received, outgoing the ones I sent")
	void findRequests_splitByDirection() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		User c = testUsers.create();
		User friend = testUsers.create();
		testFriends.sendRequest(b, a);
		testFriends.sendRequest(a, c);
		testFriends.makeFriends(friend, a);

		// Act
		List<FriendRequestResponse> incoming = friendshipService.findIncomingRequests(a.getId());
		List<FriendRequestResponse> outgoing = friendshipService.findOutgoingRequests(a.getId());

		// Assert: l'amicizia ACCEPTED non compare in nessuna delle due liste
		assertThat(incoming).extracting(FriendRequestResponse::requesterId).containsExactly(b.getId());
		assertThat(outgoing).extracting(FriendRequestResponse::recipientId).containsExactly(c.getId());
	}

	// --- rimozione ---

	@Test
	@DisplayName("Removing a friend deletes the row, from either side")
	void remove_friend_deletesRow() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.makeFriends(a, b);

		// Act: la rimozione la fa chi aveva ricevuto la richiesta
		friendshipService.remove(b.getId(), a.getId());

		// Assert
		assertThat(testFriends.countRowsBetween(a, b)).isZero();
		assertThat(friendshipService.findFriends(a.getId())).isEmpty();
	}

	@Test
	@DisplayName("The requester can withdraw a pending request")
	void remove_pendingByRequester_deletesRow() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act
		friendshipService.remove(a.getId(), b.getId());

		// Assert
		assertThat(testFriends.countRowsBetween(a, b)).isZero();
	}

	@Test
	@DisplayName("Rule 7: removing a friend deletes the PENDING battle invites between the two, in both directions")
	void remove_friend_deletesPendingBattleInvitesBetweenThem() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		User other = testUsers.create();
		testFriends.makeFriends(a, b);
		UUID fromA = testFriends.createBattleInvite(a, b, "PENDING");
		UUID fromB = testFriends.createBattleInvite(b, a, "PENDING");
		UUID accepted = testFriends.createBattleInvite(a, b, "ACCEPTED");
		UUID withOther = testFriends.createBattleInvite(a, other, "PENDING");

		// Act
		friendshipService.remove(a.getId(), b.getId());

		// Assert: spariscono solo i PENDING tra A e B; l'accettato e quello verso altri restano
		assertThat(testFriends.battleInviteExists(fromA)).isFalse();
		assertThat(testFriends.battleInviteExists(fromB)).isFalse();
		assertThat(testFriends.battleInviteExists(accepted)).isTrue();
		assertThat(testFriends.battleInviteExists(withOther)).isTrue();
	}

	@Test
	@DisplayName("Removing a relationship that does not exist is rejected")
	void remove_noRelationship_throwsNotFound() {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();

		// Act + Assert
		assertThatThrownBy(() -> friendshipService.remove(a.getId(), b.getId()))
			.isInstanceOf(FriendshipNotFoundException.class);
	}

	@Test
	@DisplayName("Removing yourself is rejected")
	void remove_self_throwsSelfRequest() {
		// Arrange
		User a = testUsers.create();

		// Act + Assert
		assertThatThrownBy(() -> friendshipService.remove(a.getId(), a.getId()))
			.isInstanceOf(FriendshipSelfRequestException.class);
	}

	// --- supporto ---

	/** Esito di una chiamata concorrente: {@code conflict} se ha ricevuto FriendshipAlreadyExistsException. */
	private record Outcome(boolean conflict) {
	}

	/**
	 * Esegue le due chiamate nello stesso momento, ognuna nella propria transazione, e ne
	 * restituisce l'esito. Un errore diverso dal conflitto fa fallire il test.
	 */
	private List<Outcome> runConcurrently(Callable<?> first, Callable<?> second) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Outcome> f1 = executor.submit(() -> attempt(start, first));
			Future<Outcome> f2 = executor.submit(() -> attempt(start, second));
			start.countDown();
			return List.of(f1.get(10, TimeUnit.SECONDS), f2.get(10, TimeUnit.SECONDS));
		} finally {
			executor.shutdownNow();
		}
	}

	private static Outcome attempt(CountDownLatch start, Callable<?> call) throws Exception {
		start.await();
		try {
			call.call();
			return new Outcome(false);
		} catch (FriendshipAlreadyExistsException ex) {
			return new Outcome(true);
		}
	}
}
