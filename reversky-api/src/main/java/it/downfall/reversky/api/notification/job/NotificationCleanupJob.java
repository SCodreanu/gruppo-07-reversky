package it.downfall.reversky.api.notification.job;

import java.time.Clock;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import it.downfall.reversky.api.notification.service.NotificationProperties;
import it.downfall.reversky.api.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Elimina ogni notte le notifiche già lette più vecchie di {@code reversky.notifications.retention} (docs/notifiche,
 * "Pulizia"). Le notifiche non lette restano sempre: l'utente non le ha ancora viste.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupJob {

	private final NotificationService notificationService;
	private final NotificationProperties properties;
	private final Clock clock;

	/**
	 * Ogni giorno alle 3:30, mezz'ora dopo la pulizia dei token, quando il traffico è più basso.
	 */
	@Scheduled(cron = "0 30 3 * * *")
	public void deleteOldReadNotifications() {
		Instant before = Instant.now(clock).minus(properties.retention());
		int deleted = notificationService.deleteReadCreatedBefore(before);
		log.info("Deleted {} read notifications created before {}", deleted, before);
	}
}
