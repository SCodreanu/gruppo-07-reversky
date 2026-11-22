package it.downfall.reversky.api.notification.job;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.api.notification.service.NotificationProperties;
import it.downfall.reversky.api.notification.service.NotificationService;

/**
 * {@link NotificationCleanupJob}: il limite è adesso meno {@code reversky.notifications.retention}. Che la query tocchi
 * solo le notifiche lette e vecchie è in {@code NotificationServiceIT}.
 */
class NotificationCleanupJobTest {

	private static final Instant NOW = Instant.parse("2026-11-22T03:30:00Z");

	@Test
	@DisplayName("The nightly cleanup deletes read notifications created before now minus the retention")
	void deleteOldReadNotifications_usesRetention() {
		// Arrange
		NotificationService service = mock(NotificationService.class);
		NotificationCleanupJob job = new NotificationCleanupJob(service, new NotificationProperties(Duration.ofDays(30)),
			Clock.fixed(NOW, ZoneOffset.UTC));

		// Act
		job.deleteOldReadNotifications();

		// Assert
		verify(service).deleteReadCreatedBefore(Instant.parse("2026-10-23T03:30:00Z"));
	}
}
