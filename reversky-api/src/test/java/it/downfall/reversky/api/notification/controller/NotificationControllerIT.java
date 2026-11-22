package it.downfall.reversky.api.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.notification.entity.NotificationType;
import it.downfall.reversky.api.notification.service.NotificationService;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Notifiche via HTTP (API-65, docs/notifiche): lista dalla più recente con il filtro delle non lette, contatore,
 * lettura di una e di tutte, sempre solo per le proprie notifiche.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerIT {

	private static final String PATH = "/api/v1/notifications";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

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

	@Test
	@DisplayName("The list has only my notifications, newest first, with type, payload, read flag and creation time")
	void findMine_onlyMineNewestFirst() throws Exception {
		// Arrange
		Integer older = notify(user, NotificationType.FRIEND_REQUEST_RECEIVED, Map.of("userId", other.getId(),
			"username", other.getUsername()));
		Integer newer = notify(user, NotificationType.TRADE_PROPOSED, Map.of("tradeId", "t-1", "userId", other.getId(),
			"username", other.getUsername()));
		notify(other, NotificationType.TRADE_ACCEPTED, Map.of("tradeId", "t-2"));

		// Act + Assert
		mockMvc.perform(as(user, get(PATH)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.content[0].id").value(newer))
			.andExpect(jsonPath("$.content[0].type").value("TRADE_PROPOSED"))
			.andExpect(jsonPath("$.content[0].payload.tradeId").value("t-1"))
			.andExpect(jsonPath("$.content[0].payload.username").value(other.getUsername()))
			.andExpect(jsonPath("$.content[0].read").value(false))
			.andExpect(jsonPath("$.content[0].createdAt").isNotEmpty())
			.andExpect(jsonPath("$.content[1].id").value(older))
			.andExpect(jsonPath("$.content[1].type").value("FRIEND_REQUEST_RECEIVED"));
	}

	@Test
	@DisplayName("With unread=true only unread notifications are listed; pages follow the same order")
	void findMine_unreadOnlyAndPaging() throws Exception {
		// Arrange
		Integer first = notify(user, NotificationType.MATCH_FOUND, Map.of("battleId", "b-1"));
		Integer second = notify(user, NotificationType.MATCH_FOUND, Map.of("battleId", "b-2"));
		Integer third = notify(user, NotificationType.MATCH_FOUND, Map.of("battleId", "b-3"));
		mockMvc.perform(as(user, post(PATH + "/" + second + "/read"))).andExpect(status().isNoContent());

		// Act + Assert
		mockMvc.perform(as(user, get(PATH)).param("unread", "true"))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.content[0].id").value(third))
			.andExpect(jsonPath("$.content[1].id").value(first));
		mockMvc.perform(as(user, get(PATH)).param("size", "1").param("page", "1"))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.totalPages").value(3))
			.andExpect(jsonPath("$.content[0].id").value(second))
			.andExpect(jsonPath("$.content[0].read").value(true));
	}

	@Test
	@DisplayName("The unread count follows reads, and another user's notifications never count")
	void countUnread_followsReads() throws Exception {
		// Arrange
		Integer first = notify(user, NotificationType.BATTLE_COMPLETED, Map.of("battleId", "b-1"));
		notify(user, NotificationType.BATTLE_COMPLETED, Map.of("battleId", "b-2"));
		notify(other, NotificationType.BATTLE_COMPLETED, Map.of("battleId", "b-3"));

		// Act + Assert
		mockMvc.perform(as(user, get(PATH + "/unread-count")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.count").value(2));
		mockMvc.perform(as(user, post(PATH + "/" + first + "/read"))).andExpect(status().isNoContent());
		mockMvc.perform(as(user, get(PATH + "/unread-count"))).andExpect(jsonPath("$.count").value(1));
	}

	@Test
	@DisplayName("Reading is idempotent and keeps the first read time")
	void markRead_twice_keepsFirstReadTime() throws Exception {
		// Arrange
		Integer id = notify(user, NotificationType.TRADE_REJECTED, Map.of("tradeId", "t-1"));
		mockMvc.perform(as(user, post(PATH + "/" + id + "/read"))).andExpect(status().isNoContent());
		Object firstRead = readAt(id);

		// Act + Assert
		mockMvc.perform(as(user, post(PATH + "/" + id + "/read"))).andExpect(status().isNoContent());
		assertThat(readAt(id)).isNotNull().isEqualTo(firstRead);
	}

	@Test
	@DisplayName("Another user's notification and a missing one are 404 NOTIFICATION_NOT_FOUND and stay unread")
	void markRead_notMine_returns404() throws Exception {
		// Arrange
		Integer theirs = notify(other, NotificationType.TRADE_PROPOSED, Map.of("tradeId", "t-1"));

		// Act + Assert
		mockMvc.perform(as(user, post(PATH + "/" + theirs + "/read")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
		mockMvc.perform(as(user, post(PATH + "/999999999/read")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
		assertThat(readAt(theirs)).isNull();
	}

	@Test
	@DisplayName("Read-all marks all my notifications as read and leaves another user's alone")
	void markAllRead_onlyMine() throws Exception {
		// Arrange
		notify(user, NotificationType.FRIEND_REQUEST_ACCEPTED, Map.of("userId", other.getId()));
		notify(user, NotificationType.BATTLE_INVITE_RECEIVED, Map.of("inviteId", "i-1"));
		Integer theirs = notify(other, NotificationType.BATTLE_INVITE_RECEIVED, Map.of("inviteId", "i-2"));

		// Act
		mockMvc.perform(as(user, post(PATH + "/read-all"))).andExpect(status().isNoContent());

		// Assert
		mockMvc.perform(as(user, get(PATH + "/unread-count"))).andExpect(jsonPath("$.count").value(0));
		mockMvc.perform(as(user, get(PATH)).param("unread", "true")).andExpect(jsonPath("$.totalElements").value(0));
		assertThat(readAt(theirs)).isNull();
		mockMvc.perform(as(user, post(PATH + "/read-all"))).andExpect(status().isNoContent());
	}

	// notify vuole la transazione del chiamante, come nelle altre funzionalità
	private Integer notify(User who, NotificationType type, Map<String, Object> payload) {
		return transactionTemplate.execute(status -> notificationService.notify(who.getId(), type, payload)).id();
	}

	private Object readAt(Integer notificationId) {
		return jdbcTemplate.queryForObject("SELECT read_at FROM notifications WHERE id = ?", Object.class,
			notificationId);
	}

	private MockHttpServletRequestBuilder as(User who, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(who));
	}
}
