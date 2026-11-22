package it.downfall.reversky.api.notification.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

/**
 * Configurazione tecnica delle notifiche (docs/notifiche): non sono regole di gioco.
 *
 * @param retention per quanto tempo una notifica già letta resta nel database, dalla sua creazione; quelle non lette
 *                  non si cancellano mai
 */
@Validated
@ConfigurationProperties(prefix = "reversky.notifications")
public record NotificationProperties(

	@NotNull
	Duration retention
) {
}
