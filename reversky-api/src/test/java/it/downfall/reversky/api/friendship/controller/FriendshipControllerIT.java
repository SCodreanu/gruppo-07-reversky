package it.downfall.reversky.api.friendship.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
 */
@Import({TestcontainersConfiguration.class, TestUsers.class, TestFriends.class})
@SpringBootTest
@AutoConfigureMockMvc
class FriendshipControllerIT {

	private static final String BASE_PATH = "/api/v1/friends";

	@Autowired private MockMvc mockMvc;
	@Autowired private JwtService jwtService;
	@Autowired private TestUsers testUsers;
	@Autowired private TestFriends testFriends;
	@Autowired private FriendshipService friendshipService;

	@MockitoBean private EmailSender emailSender;

	// --- accesso ---

	@Test
	@DisplayName("GET /friends without a token returns 401")
	void findFriends_withoutToken_returns401() throws Exception {
		mockMvc.perform(get(BASE_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	// --- GET ---

	@Test
	@DisplayName("GET /friends returns the accepted friends")
	void findFriends_returnsAccepted() throws Exception {
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.makeFriends(a, b);

		mockMvc.perform(asUser(a, get(BASE_PATH)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].userId").value(b.getId()))
			.andExpect(jsonPath("$[0].username").value(b.getUsername()));
	}

	@Test
	@DisplayName("GET /friends/requests?box=incoming returns pending requests where I am the recipient")
	void findIncoming_returnsPending() throws Exception {
		User a = testUsers.create();
		User b = testUsers.create();
		friendshipService.sendRequest(a.getId(), b.getUsername());

		mockMvc.perform(asUser(b, get(BASE_PATH + "/requests").param("box", "incoming")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].requesterId").value(a.getId()))
			.andExpect(jsonPath("$[0].recipientId").value(b.getId()))
			.andExpect(jsonPath("$[0].status").value("PENDING"));
	}

	@Test
	@DisplayName("GET /friends/requests?box=outgoing returns pending requests where I am the requester")
	void findOutgoing_returnsPending() throws Exception {
		User a = testUsers.create();
		User b = testUsers.create();
		friendshipService.sendRequest(a.getId(), b.getUsername());

		mockMvc.perform(asUser(a, get(BASE_PATH + "/requests").param("box", "outgoing")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].requesterId").value(a.getId()))
			.andExpect(jsonPath("$[0].recipientId").value(b.getId()));
	}

	@Test
	@DisplayName("GET /friends/requests with an invalid box returns 400")
	void findRequests_invalidBox_returns400() throws Exception {
		User a = testUsers.create();

		mockMvc.perform(asUser(a, get(BASE_PATH + "/requests").param("box", "wrong")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_FRIENDSHIP_BOX"));
	}

	// --- POST /requests ---

	@Test
	@DisplayName("POST /friends/requests creates a PENDING request")
	void sendRequest_valid_returns200() throws Exception {
		User a = testUsers.create();
		User b = testUsers.create();

		mockMvc.perform(asUser(a, post(BASE_PATH + "/requests"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"" + b.getUsername() + "\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.requesterId").value(a.getId()))
			.andExpect(jsonPath("$.recipientId").value(b.getId()))
			.andExpect(jsonPath("$.status").value("PENDING"));
	}

	@Test
	@DisplayName("POST /friends/requests to yourself returns 400")
	void sendRequest_toSelf_returns400() throws Exception {
		User a = testUsers.create();

		mockMvc.perform(asUser(a, post(BASE_PATH + "/requests"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"" + a.getUsername() + "\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FRIENDSHIP_SELF_REQUEST"));
	}

	@Test
	@DisplayName("POST /friends/requests with an unknown username returns 404")
	void sendRequest_unknownUsername_returns404() throws Exception {
		User a = testUsers.create();

		mockMvc.perform(asUser(a, post(BASE_PATH + "/requests"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"nonexistent_user_xyz\"}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	@DisplayName("POST /friends/requests when the row already exists returns 409")
	void sendRequest_alreadyExists_returns409() throws Exception {
		User a = testUsers.create();
		User b = testUsers.create();
		friendshipService.sendRequest(a.getId(), b.getUsername());

		mockMvc.perform(asUser(a, post(BASE_PATH + "/requests"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"" + b.getUsername() + "\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("FRIENDSHIP_ALREADY_EXISTS"));
	}

	@Test
	@DisplayName("POST /friends/requests opposite to a PENDING one becomes ACCEPTED")
	void sendRequest_opposite_autoAccepts() throws Exception {
		User a = testUsers.create();
		User b = testUsers.create();
		friendshipService.sendRequest(a.getId(), b.getUsername());

		mockMvc.perform(asUser(b, post(BASE_PATH + "/requests"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"" + a.getUsername() + "\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACCEPTED"));
	}

	// --- accept / reject ---

	@Test
	@DisplayName("POST /friends/requests/{requesterId}/accept by the recipient returns 200 ACCEPTED")
	void accept_recipient_returns200() throws Exception {
		User a = testUsers.create();
		User b = testUsers.create();
		friendshipService.sendRequest(a.getId(), b.getUsername());

		mockMvc.perform(asUser(b, post(BASE_PATH + "/requests/" + a.getId() + "/accept")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACCEPTED"));
	}

	@Test
	@DisplayName("POST accept by someone who is not the recipient returns 404")
	void accept_nonRecipient_returns404() throws Exception {
		User a = testUsers.create();
		User b = testUsers.create();
		User c = testUsers.create();
		friendshipService.sendRequest(a.getId(), b.getUsername());

		mockMvc.perform(asUser(c, post(BASE_PATH + "/requests/" + a.getId() + "/accept")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FRIENDSHIP_REQUEST_NOT_FOUND"));
	}

	@Test
	@DisplayName("POST reject by the recipient returns 204")
	void reject_recipient_returns204() throws Exception {
		User a = testUsers.create();
		User b = testUsers.create();
		friendshipService.sendRequest(a.getId(), b.getUsername());

		mockMvc.perform(asUser(b, post(BASE_PATH + "/requests/" + a.getId() + "/reject")))
			.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("POST reject by someone who is not the recipient returns 404")
	void reject_nonRecipient_returns404() throws Exception {
		User a = testUsers.create();
		User b = testUsers.create();
		User c = testUsers.create();
		friendshipService.sendRequest(a.getId(), b.getUsername());

		mockMvc.perform(asUser(c, post(BASE_PATH + "/requests/" + a.getId() + "/reject")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FRIENDSHIP_REQUEST_NOT_FOUND"));
	}

	// --- DELETE ---

	@Test
	@DisplayName("DELETE /friends/{userId} removes the friendship")
	void remove_friend_returns204() throws Exception {
		User a = testUsers.create();
		User b = testUsers.create();
		testFriends.makeFriends(a, b);

		mockMvc.perform(asUser(a, delete(BASE_PATH + "/" + b.getId())))
			.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("DELETE /friends/{userId} with no relationship returns 404")
	void remove_unknown_returns404() throws Exception {
		User a = testUsers.create();
		User b = testUsers.create();

		mockMvc.perform(asUser(a, delete(BASE_PATH + "/" + b.getId())))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FRIENDSHIP_NOT_FOUND"));
	}

	@Test
	@DisplayName("DELETE /friends/{myId} (myself) returns 400")
	void remove_self_returns400() throws Exception {
		User a = testUsers.create();

		mockMvc.perform(asUser(a, delete(BASE_PATH + "/" + a.getId())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FRIENDSHIP_SELF_REQUEST"));
	}

	// --- supporto ---

	private MockHttpServletRequestBuilder asUser(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}
}