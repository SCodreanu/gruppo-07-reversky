package it.downfall.reversky.api.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.notification.entity.NotificationType;
import it.downfall.reversky.api.notification.service.NotificationService;
import it.downfall.reversky.api.notification.service.NotificationStreamService;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Canale SSE delle notifiche via HTTP (API-66, docs/notifiche, "Canale SSE"): un evento arriva solo al destinatario,
 * su tutte le sue connessioni e solo dopo il commit; {@code Last-Event-ID} rimanda le notifiche perse; il ping; un
 * account disattivato perde le connessioni e non ne apre di nuove.
 * <p>
 * Con MockMvc la risposta di una connessione aperta si legge mentre cresce: ogni evento è già scritto quando
 * {@code notify} ha fatto il commit.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class NotificationStreamIT {

	private static final String STREAM = "/api/v1/notifications/stream";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private NotificationStreamService streamService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	private User user;
	private User other;

	@BeforeEach
	void setUp() {
		user = testUsers.create();
		other = testUsers.create();
	}

	@AfterEach
	void closeStreams() {
		streamService.disconnect(user.getId());
		streamService.disconnect(other.getId());
	}

	@Test
	@DisplayName("A notification reaches every open stream of its user, with id, type as event name and JSON data")
	void notify_reachesAllMyStreamsOnly() throws Exception {
		// Arrange: due schede aperte per l'utente, una per un altro
		MvcResult firstTab = open(user, null);
		MvcResult secondTab = open(user, null);
		MvcResult otherStream = open(other, null);
		assertThat(streamService.connections(user.getId())).isEqualTo(2);

		// Act
		Integer id = notify(user, NotificationType.TRADE_PROPOSED, Map.of("tradeId", "t-1", "userId", other.getId(),
			"username", other.getUsername()));

		// Assert
		for (MvcResult tab : new MvcResult[] {firstTab, secondTab}) {
			assertThat(content(tab))
				.contains("id:" + id + "\n")
				.contains("event:TRADE_PROPOSED\n")
				.contains("\"type\":\"TRADE_PROPOSED\"")
				.contains("\"username\":\"" + other.getUsername() + "\"")
				.contains("\"read\":false");
		}
		assertThat(content(otherStream)).doesNotContain("TRADE_PROPOSED");
		assertThat(firstTab.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
	}

	@Test
	@DisplayName("A notification created in a transaction that rolls back sends no event")
	void notify_rolledBack_sendsNothing() throws Exception {
		// Arrange
		MvcResult stream = open(user, null);

		// Act
		transactionTemplate.executeWithoutResult(status -> {
			notificationService.notify(user.getId(), NotificationType.MATCH_FOUND, Map.of("battleId", "b-1"));
			status.setRollbackOnly();
		});

		// Assert
		assertThat(content(stream)).doesNotContain("MATCH_FOUND");
	}

	@Test
	@DisplayName("Reconnecting with Last-Event-ID sends the notifications created after it, oldest first")
	void open_withLastEventId_replaysMissed() throws Exception {
		// Arrange: tre notifiche mentre il client era scollegato, la prima già ricevuta
		Integer first = notify(user, NotificationType.BATTLE_COMPLETED, Map.of("battleId", "b-1"));
		Integer second = notify(user, NotificationType.BATTLE_COMPLETED, Map.of("battleId", "b-2"));
		Integer third = notify(user, NotificationType.BATTLE_COMPLETED, Map.of("battleId", "b-3"));
		notify(other, NotificationType.BATTLE_COMPLETED, Map.of("battleId", "b-4"));

		// Act
		String content = content(open(user, first));

		// Assert
		assertThat(content).doesNotContain("id:" + first + "\n").doesNotContain("b-4");
		assertThat(content.indexOf("id:" + second + "\n")).isNotNegative()
			.isLessThan(content.indexOf("id:" + third + "\n"));
	}

	@Test
	@DisplayName("The heartbeat writes a ping comment on every open stream")
	void heartbeat_writesPing() throws Exception {
		// Arrange
		MvcResult stream = open(user, null);

		// Act
		streamService.sendHeartbeat();

		// Assert
		assertThat(content(stream)).contains(":ping\n");
	}

	@Test
	@DisplayName("Disabling an account closes its open streams; the stream completes and passes security again")
	void disable_closesStreams() throws Exception {
		// Arrange
		MvcResult stream = open(user, null);
		User admin = testUsers.createAdmin();

		// Act
		mockMvc.perform(as(admin, post("/api/v1/admin/users/" + user.getId() + "/disable")))
			.andExpect(status().isOk());

		// Assert: nessuna connessione resta e il completamento della risposta asincrona non viene bloccato
		assertThat(streamService.connections(user.getId())).isZero();
		mockMvc.perform(asyncDispatch(stream)).andExpect(status().isOk());
		Integer id = notify(user, NotificationType.TRADE_ACCEPTED, Map.of("tradeId", "t-1"));
		assertThat(content(stream)).doesNotContain("id:" + id + "\n");
	}

	@Test
	@DisplayName("A disabled account with a still valid access token cannot open the stream: 403 AUTH_ACCOUNT_DISABLED")
	void open_disabledAccount_returns403() throws Exception {
		// Arrange: il token è stato creato prima della disattivazione
		String token = jwtService.createAccessToken(user);
		testUsers.disable(user);

		// Act + Assert
		mockMvc.perform(get(STREAM).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.accept(MediaType.TEXT_EVENT_STREAM))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("AUTH_ACCOUNT_DISABLED"));
		assertThat(streamService.connections(user.getId())).isZero();
	}

	@Test
	@DisplayName("A Last-Event-ID that is not a number is 400 and opens nothing")
	void open_invalidLastEventId_returns400() throws Exception {
		// Act + Assert
		mockMvc.perform(as(user, get(STREAM)).header("Last-Event-ID", "abc"))
			.andExpect(status().isBadRequest());
		assertThat(streamService.connections(user.getId())).isZero();
	}

	private MvcResult open(User who, Integer lastEventId) throws Exception {
		MockHttpServletRequestBuilder builder = as(who, get(STREAM)).accept(MediaType.TEXT_EVENT_STREAM);
		if (lastEventId != null) {
			builder.header("Last-Event-ID", lastEventId);
		}
		return mockMvc.perform(builder)
			.andExpect(request().asyncStarted())
			.andReturn();
	}

	// notify vuole la transazione del chiamante; il listener invia l'evento al commit
	private Integer notify(User who, NotificationType type, Map<String, Object> payload) {
		return transactionTemplate.execute(status -> notificationService.notify(who.getId(), type, payload)).id();
	}

	private static String content(MvcResult result) throws Exception {
		return result.getResponse().getContentAsString();
	}

	private MockHttpServletRequestBuilder as(User who, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(who));
	}
}
