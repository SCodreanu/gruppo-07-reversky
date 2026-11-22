package it.downfall.reversky.api.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import it.downfall.reversky.api.notification.repository.NotificationRepository;
import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.service.UserService;

/**
 * {@link NotificationStreamService} senza HTTP (API-66): la connessione dura quanto l'access token che l'ha aperta.
 * Gli invii veri sono in {@code NotificationStreamIT}.
 */
class NotificationStreamServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

	private final UserService userService = mock(UserService.class);
	private final NotificationStreamService streamService = new NotificationStreamService(
		mock(NotificationRepository.class), userService, Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	@DisplayName("The stream times out when the access token that opened it expires")
	void open_timeoutIsTokenLifetime() {
		// Arrange
		enabledUser(7);

		// Act
		SseEmitter emitter = streamService.open(7, null, NOW.plusSeconds(90));

		// Assert
		assertThat(emitter.getTimeout()).isEqualTo(90_000L);
		assertThat(streamService.connections(7)).isEqualTo(1);
	}

	@Test
	@DisplayName("A token already at its expiry gives the shortest timeout, never zero (which would mean no timeout)")
	void open_expiredToken_minimalTimeout() {
		// Arrange
		enabledUser(7);

		// Act
		SseEmitter emitter = streamService.open(7, null, NOW.minusSeconds(1));

		// Assert
		assertThat(emitter.getTimeout()).isEqualTo(1L);
	}

	@Test
	@DisplayName("Disconnecting removes every stream of the user")
	void disconnect_removesAll() {
		// Arrange
		enabledUser(7);
		streamService.open(7, null, NOW.plusSeconds(60));
		streamService.open(7, null, NOW.plusSeconds(60));

		// Act
		streamService.disconnect(7);

		// Assert
		assertThat(streamService.connections(7)).isZero();
	}

	private void enabledUser(Integer id) {
		User user = mock(User.class);
		when(user.isEnabled()).thenReturn(true);
		when(userService.getById(id)).thenReturn(user);
	}
}
