package it.downfall.reversky.api.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.notification.dto.NotificationResponse;
import it.downfall.reversky.api.notification.entity.NotificationType;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * {@link NotificationService} con il database vero (API-65): la notifica nasce nella transazione del chiamante e
 * sparisce con lei, il {@code payload} jsonb torna uguale, la pulizia tocca solo le lette e vecchie.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class NotificationServiceIT {

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TestUsers testUsers;

	// Non serve al test: con la stessa configurazione dei ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("notify saves an unread notification whose jsonb payload reads back the same")
	void notify_savesUnreadWithPayload() {
		// Arrange
		User user = testUsers.create();

		// Act
		NotificationResponse saved = transactionTemplate.execute(status -> notificationService.notify(user.getId(),
			NotificationType.BATTLE_COMPLETED, Map.of("battleId", "b-1", "userId", 7, "username", "bob")));

		// Assert
		assertThat(saved.id()).isNotNull();
		assertThat(saved.read()).isFalse();
		assertThat(jdbcTemplate.queryForObject("SELECT type FROM notifications WHERE id = ?", String.class, saved.id()))
			.isEqualTo("BATTLE_COMPLETED");
		assertThat(notificationService.findMine(user.getId(), false, PageRequest.of(0, 5))
			.content()).singleElement()
			.satisfies(notification -> assertThat(notification.payload())
				.containsEntry("battleId", "b-1").containsEntry("userId", 7).containsEntry("username", "bob"));
	}

	@Test
	@DisplayName("A notification created in a transaction that rolls back is not saved")
	void notify_rolledBack_notSaved() {
		// Arrange
		User user = testUsers.create();

		// Act
		transactionTemplate.executeWithoutResult(status -> {
			notificationService.notify(user.getId(), NotificationType.TRADE_PROPOSED, Map.of("tradeId", "t-1"));
			status.setRollbackOnly();
		});

		// Assert
		assertThat(notificationService.countUnread(user.getId()).count()).isZero();
	}

	@Test
	@DisplayName("notify without the caller's transaction is a programming error")
	void notify_withoutTransaction_throws() {
		// Arrange
		User user = testUsers.create();

		// Act + Assert
		assertThatThrownBy(() -> notificationService.notify(user.getId(), NotificationType.MATCH_FOUND, Map.of()))
			.isInstanceOf(IllegalTransactionStateException.class);
	}

	@Test
	@DisplayName("The cleanup deletes only read notifications created before the limit; unread ones always stay")
	void deleteReadCreatedBefore_onlyOldRead() {
		// Arrange: una vecchia letta, una vecchia non letta, una recente letta
		User user = testUsers.create();
		Integer oldRead = notify(user);
		Integer oldUnread = notify(user);
		Integer recentRead = notify(user);
		jdbcTemplate.update("UPDATE notifications SET created_at = now() - INTERVAL '40 days' WHERE id IN (?, ?)",
			oldRead, oldUnread);
		jdbcTemplate.update("UPDATE notifications SET read_at = now() WHERE id IN (?, ?)", oldRead, recentRead);

		// Act
		int deleted = notificationService.deleteReadCreatedBefore(Instant.now().minusSeconds(30L * 24 * 3600));

		// Assert: altri test possono lasciare notifiche lette e vecchie, quindi si controllano solo le proprie
		assertThat(deleted).isPositive();
		assertThat(exists(oldRead)).isFalse();
		assertThat(exists(oldUnread)).isTrue();
		assertThat(exists(recentRead)).isTrue();
	}

	private Integer notify(User user) {
		return transactionTemplate.execute(status -> notificationService.notify(user.getId(),
			NotificationType.MATCH_FOUND, Map.of("battleId", "b"))).id();
	}

	private boolean exists(Integer notificationId) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM notifications WHERE id = ?", Integer.class,
			notificationId) == 1;
	}
}
