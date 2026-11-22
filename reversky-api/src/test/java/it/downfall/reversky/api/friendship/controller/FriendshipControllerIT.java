package it.downfall.reversky.api.friendship.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.friendship.service.FriendshipService;
import it.downfall.reversky.api.support.TestFriends;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * {@link FriendshipController} via HTTP, con i filtri di Spring Security attivi.
 * <p>
 * Si verificano accesso (401), status, forma delle risposte e {@code code} degli errori. I casi
 * limite delle regole (concorrenza, inviti di sfida, ripetizione dopo un rifiuto) sono in
 * FriendshipServiceIT.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class FriendshipControllerIT {

	private static final String BASE_PATH = "/api/v1/friends";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private FriendshipService friendshipService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa
	// il contesto già avviato (e il suo container) invece di crearne uno nuovo.
	@MockitoBean
	private EmailSender emailSender;

	private TestFriends testFriends;

	@BeforeEach
	void setUp() {
		testFriends = new TestFriends(friendshipService, jdbcTemplate);
	}

	// --- accesso ---

	@Test
	@DisplayName("Without a token the friends list returns 401")
	void findFriends_withoutToken_returns401() throws Exception {
		// Act + Assert
		mockMvc.perform(get(BASE_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("Without a token a friend request returns 401")
	void sendRequest_withoutToken_returns401() throws Exception {
		// Arrange
		User b = testUsers.create();

		// Act + Assert
		mockMvc.perform(post(BASE_PATH + "/requests").contentType(MediaType.APPLICATION_JSON).content(body(b.getUsername())))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("Without a token the remove returns 401")
	void remove_withoutToken_returns401() throws Exception {
		// Act + Assert
		mockMvc.perform(delete(BASE_PATH + "/1"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	// --- GET /friends ---

	@Test
	@DisplayName("GET /friends returns the friends with id and username, seen from both sides")
	void findFriends_friendship_returnsOtherUserOnBothSides() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.makeFriends(a, b);

		// Act + Assert: per A l'amico è B, per B è A
		mockMvc.perform(asUser(a, get(BASE_PATH)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].userId").value(b.getId()))
			.andExpect(jsonPath("$[0].username").value(b.getUsername()));
		mockMvc.perform(asUser(b, get(BASE_PATH)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].userId").value(a.getId()));
	}

	@Test
	@DisplayName("GET /friends does not list pending requests")
	void findFriends_pendingRequest_returnsEmptyList() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act + Assert
		mockMvc.perform(asUser(a, get(BASE_PATH)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
	}

	// --- GET /friends/requests ---

	@Test
	@DisplayName("box=incoming returns the pending requests I received, with both users")
	void findRequests_incoming_returnsReceived() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act + Assert
		mockMvc.perform(asUser(b, get(BASE_PATH + "/requests").param("box", "incoming")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].requesterId").value(a.getId()))
			.andExpect(jsonPath("$[0].requesterUsername").value(a.getUsername()))
			.andExpect(jsonPath("$[0].recipientId").value(b.getId()))
			.andExpect(jsonPath("$[0].recipientUsername").value(b.getUsername()))
			.andExpect(jsonPath("$[0].status").value("PENDING"))
			.andExpect(jsonPath("$[0].createdAt").isString());
	}

	@Test
	@DisplayName("Without box the incoming requests are returned")
	void findRequests_noBox_defaultsToIncoming() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act + Assert: per chi ha inviato la richiesta la lista di default è vuota
		mockMvc.perform(asUser(b, get(BASE_PATH + "/requests")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].requesterId").value(a.getId()));
		mockMvc.perform(asUser(a, get(BASE_PATH + "/requests")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	@DisplayName("box=outgoing returns the pending requests I sent")
	void findRequests_outgoing_returnsSent() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act + Assert
		mockMvc.perform(asUser(a, get(BASE_PATH + "/requests").param("box", "outgoing")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].requesterId").value(a.getId()))
			.andExpect(jsonPath("$[0].recipientId").value(b.getId()));
	}

	@Test
	@DisplayName("An unknown box returns 400")
	void findRequests_invalidBox_returns400() throws Exception {
		// Arrange
		User a = testUsers.create();

		// Act + Assert
		mockMvc.perform(asUser(a, get(BASE_PATH + "/requests").param("box", "wrong")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_FRIENDSHIP_BOX"));
	}

	// --- POST /friends/requests ---

	@Test
	@DisplayName("A friend request returns 200 with a PENDING request")
	void sendRequest_valid_returns200Pending() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();

		// Act + Assert
		postRequest(a, body(b.getUsername()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.requesterId").value(a.getId()))
			.andExpect(jsonPath("$.recipientId").value(b.getId()))
			.andExpect(jsonPath("$.recipientUsername").value(b.getUsername()))
			.andExpect(jsonPath("$.status").value("PENDING"));
		assertThat(testFriends.countRowsBetween(a, b)).isEqualTo(1);
	}

	@Test
	@DisplayName("A request opposite to a PENDING one returns 200 ACCEPTED")
	void sendRequest_oppositeToPending_returns200Accepted() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act + Assert
		postRequest(b, body(a.getUsername()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.requesterId").value(a.getId()))
			.andExpect(jsonPath("$.status").value("ACCEPTED"));
		assertThat(testFriends.countRowsBetween(a, b)).isEqualTo(1);
	}

	@Test
	@DisplayName("A blank username returns 400 VALIDATION_FAILED")
	void sendRequest_blankUsername_returns400() throws Exception {
		// Arrange
		User a = testUsers.create();

		// Act + Assert
		postRequest(a, body(""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItem("username")));
	}

	@Test
	@DisplayName("A username with forbidden characters returns 400 VALIDATION_FAILED")
	void sendRequest_malformedUsername_returns400() throws Exception {
		// Arrange
		User a = testUsers.create();

		// Act + Assert
		postRequest(a, body("bad name!"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItem("username")));
	}

	@Test
	@DisplayName("A request to yourself returns 400")
	void sendRequest_toSelf_returns400() throws Exception {
		// Arrange
		User a = testUsers.create();

		// Act + Assert
		postRequest(a, body(a.getUsername()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FRIENDSHIP_SELF_REQUEST"));
	}

	@Test
	@DisplayName("A request to an unknown username returns 404")
	void sendRequest_unknownUsername_returns404() throws Exception {
		// Arrange
		User a = testUsers.create();

		// Act + Assert
		postRequest(a, body("missing_" + TestUsers.uniqueSuffix()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	@DisplayName("A second request in the same direction returns 409")
	void sendRequest_alreadySent_returns409() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act + Assert
		postRequest(a, body(b.getUsername()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("FRIENDSHIP_ALREADY_EXISTS"));
	}

	@Test
	@DisplayName("A request between friends returns 409")
	void sendRequest_alreadyFriends_returns409() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.makeFriends(a, b);

		// Act + Assert
		postRequest(b, body(a.getUsername()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("FRIENDSHIP_ALREADY_EXISTS"));
	}

	// --- accept / reject ---

	@Test
	@DisplayName("The recipient accepts: 200 ACCEPTED, and the two users become friends")
	void accept_byRecipient_returns200() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act + Assert
		mockMvc.perform(asUser(b, post(BASE_PATH + "/requests/" + a.getId() + "/accept")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.requesterId").value(a.getId()))
			.andExpect(jsonPath("$.recipientId").value(b.getId()))
			.andExpect(jsonPath("$.status").value("ACCEPTED"));
		assertThat(friendshipService.findFriends(a.getId())).hasSize(1);
	}

	@Test
	@DisplayName("The requester cannot accept their own request: 404")
	void accept_byRequester_returns404() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act + Assert
		mockMvc.perform(asUser(a, post(BASE_PATH + "/requests/" + b.getId() + "/accept")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FRIENDSHIP_REQUEST_NOT_FOUND"));
	}

	@Test
	@DisplayName("A third user cannot accept someone else's request: 404")
	void accept_byThirdUser_returns404() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		User c = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act + Assert
		mockMvc.perform(asUser(c, post(BASE_PATH + "/requests/" + a.getId() + "/accept")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FRIENDSHIP_REQUEST_NOT_FOUND"));
	}

	@Test
	@DisplayName("The recipient rejects: 204 and the request disappears")
	void reject_byRecipient_returns204() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act + Assert
		mockMvc.perform(asUser(b, post(BASE_PATH + "/requests/" + a.getId() + "/reject")))
			.andExpect(status().isNoContent());
		assertThat(testFriends.countRowsBetween(a, b)).isZero();
	}

	@Test
	@DisplayName("A third user cannot reject someone else's request: 404")
	void reject_byThirdUser_returns404() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		User c = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act + Assert
		mockMvc.perform(asUser(c, post(BASE_PATH + "/requests/" + a.getId() + "/reject")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FRIENDSHIP_REQUEST_NOT_FOUND"));
		assertThat(testFriends.countRowsBetween(a, b)).isEqualTo(1);
	}

	// --- DELETE /friends/{userId} ---

	@Test
	@DisplayName("Removing a friend returns 204 and deletes the PENDING battle invites between the two")
	void remove_friend_returns204AndDeletesInvites() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.makeFriends(a, b);
		UUID invite = testFriends.createBattleInvite(b, a, "PENDING");

		// Act + Assert
		mockMvc.perform(asUser(a, delete(BASE_PATH + "/" + b.getId())))
			.andExpect(status().isNoContent());
		assertThat(testFriends.countRowsBetween(a, b)).isZero();
		assertThat(testFriends.battleInviteExists(invite)).isFalse();
	}

	@Test
	@DisplayName("The requester withdraws a pending request: 204")
	void remove_pendingRequest_returns204() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.sendRequest(a, b);

		// Act + Assert
		mockMvc.perform(asUser(a, delete(BASE_PATH + "/" + b.getId())))
			.andExpect(status().isNoContent());
		assertThat(testFriends.countRowsBetween(a, b)).isZero();
	}

	@Test
	@DisplayName("Removing a user who is not a friend returns 404")
	void remove_noRelationship_returns404() throws Exception {
		// Arrange
		User a = testUsers.create();
		User b = testUsers.create();

		// Act + Assert
		mockMvc.perform(asUser(a, delete(BASE_PATH + "/" + b.getId())))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FRIENDSHIP_NOT_FOUND"));
	}

	@Test
	@DisplayName("Removing yourself returns 400")
	void remove_self_returns400() throws Exception {
		// Arrange
		User a = testUsers.create();

		// Act + Assert
		mockMvc.perform(asUser(a, delete(BASE_PATH + "/" + a.getId())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FRIENDSHIP_SELF_REQUEST"));
	}

	// --- supporto ---

	private MockHttpServletRequestBuilder asUser(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}

	private ResultActions postRequest(User user, String body) throws Exception {
		return mockMvc.perform(asUser(user, post(BASE_PATH + "/requests")).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static String body(String username) {
		return "{\"username\":\"" + username + "\"}";
	}
}
