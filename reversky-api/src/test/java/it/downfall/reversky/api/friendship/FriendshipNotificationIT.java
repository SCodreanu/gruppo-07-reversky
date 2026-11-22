package it.downfall.reversky.api.friendship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.notification.service.NotificationStreamService;
import it.downfall.reversky.api.support.TestNotifications;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Notifiche delle amicizie via HTTP (API-67, docs/notifiche): richiesta ricevuta al destinatario, richiesta accettata a
 * chi l'ha mandata (anche con la richiesta inversa), niente per rifiuto, ritiro, rimozione e richiesta doppia.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class FriendshipNotificationIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NotificationStreamService streamService;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	private TestNotifications testNotifications;
	private User requester;
	private User recipient;

	@BeforeEach
	void setUp() {
		testNotifications = new TestNotifications(jdbcTemplate);
		requester = testUsers.create();
		recipient = testUsers.create();
	}

	@AfterEach
	void closeStreams() {
		streamService.disconnect(recipient.getId());
	}

	@Test
	@DisplayName("A friend request notifies the recipient, also on the open stream, with the requester's id and username")
	void sendRequest_notifiesRecipient() throws Exception {
		// Arrange: il destinatario ha il canale aperto
		MvcResult stream = mockMvc.perform(as(recipient, get("/api/v1/notifications/stream"))
				.accept(MediaType.TEXT_EVENT_STREAM))
			.andExpect(request().asyncStarted())
			.andReturn();

		// Act
		sendRequest(requester, recipient).andExpect(status().isOk());

		// Assert
		assertThat(testNotifications.types(recipient)).containsExactly("FRIEND_REQUEST_RECEIVED");
		assertThat(testNotifications.lastPayload(recipient, "FRIEND_REQUEST_RECEIVED"))
			.contains("\"userId\": " + requester.getId())
			.contains("\"username\": \"" + requester.getUsername() + "\"");
		assertThat(testNotifications.types(requester)).isEmpty();
		assertThat(stream.getResponse().getContentAsString())
			.contains("event:FRIEND_REQUEST_RECEIVED\n")
			.contains("\"username\":\"" + requester.getUsername() + "\"");
	}

	@Test
	@DisplayName("Accepting notifies the requester with the recipient's id and username")
	void accept_notifiesRequester() throws Exception {
		// Arrange
		sendRequest(requester, recipient).andExpect(status().isOk());

		// Act
		mockMvc.perform(as(recipient, post("/api/v1/friends/requests/" + requester.getId() + "/accept")))
			.andExpect(status().isOk());

		// Assert
		assertThat(testNotifications.types(requester)).containsExactly("FRIEND_REQUEST_ACCEPTED");
		assertThat(testNotifications.lastPayload(requester, "FRIEND_REQUEST_ACCEPTED"))
			.contains("\"userId\": " + recipient.getId())
			.contains("\"username\": \"" + recipient.getUsername() + "\"");
		assertThat(testNotifications.types(recipient)).containsExactly("FRIEND_REQUEST_RECEIVED");
	}

	@Test
	@DisplayName("A request in the opposite direction accepts the pending one: the first requester gets ACCEPTED")
	void reverseRequest_notifiesAccepted() throws Exception {
		// Arrange
		sendRequest(requester, recipient).andExpect(status().isOk());

		// Act: il destinatario manda a sua volta una richiesta, che accetta quella esistente
		sendRequest(recipient, requester).andExpect(status().isOk());

		// Assert
		assertThat(testNotifications.types(requester)).containsExactly("FRIEND_REQUEST_ACCEPTED");
		assertThat(testNotifications.lastPayload(requester, "FRIEND_REQUEST_ACCEPTED"))
			.contains("\"username\": \"" + recipient.getUsername() + "\"");
		assertThat(testNotifications.types(recipient)).containsExactly("FRIEND_REQUEST_RECEIVED");
	}

	@Test
	@DisplayName("Rejecting, withdrawing, removing a friend and a duplicate request create no notification")
	void reject_withdraw_remove_duplicate_notifyNothing() throws Exception {
		// Rifiuto
		sendRequest(requester, recipient).andExpect(status().isOk());
		mockMvc.perform(as(recipient, post("/api/v1/friends/requests/" + requester.getId() + "/reject")))
			.andExpect(status().isNoContent());

		// Richiesta doppia (409) e ritiro
		sendRequest(requester, recipient).andExpect(status().isOk());
		sendRequest(requester, recipient).andExpect(status().isConflict());
		mockMvc.perform(as(requester, delete("/api/v1/friends/" + recipient.getId()))).andExpect(status().isNoContent());

		// Rimozione di un'amicizia
		sendRequest(requester, recipient).andExpect(status().isOk());
		mockMvc.perform(as(recipient, post("/api/v1/friends/requests/" + requester.getId() + "/accept")))
			.andExpect(status().isOk());
		mockMvc.perform(as(recipient, delete("/api/v1/friends/" + requester.getId()))).andExpect(status().isNoContent());

		// Assert: solo le tre richieste ricevute e l'unica accettazione
		assertThat(testNotifications.types(recipient)).containsExactly("FRIEND_REQUEST_RECEIVED",
			"FRIEND_REQUEST_RECEIVED", "FRIEND_REQUEST_RECEIVED");
		assertThat(testNotifications.types(requester)).containsExactly("FRIEND_REQUEST_ACCEPTED");
	}

	private ResultActions sendRequest(User from, User to) throws Exception {
		return mockMvc.perform(as(from, post("/api/v1/friends/requests")).contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\": \"" + to.getUsername() + "\"}"));
	}

	private MockHttpServletRequestBuilder as(User who, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(who));
	}
}
