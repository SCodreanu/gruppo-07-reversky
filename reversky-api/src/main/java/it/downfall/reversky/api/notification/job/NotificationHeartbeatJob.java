package it.downfall.reversky.api.notification.job;

import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import it.downfall.reversky.api.notification.service.NotificationStreamService;
import lombok.RequiredArgsConstructor;

/**
 * Ogni 30 secondi un {@code : ping} su ogni connessione SSE aperta (docs/notifiche, "Canale SSE", API-66): i proxy
 * chiudono le connessioni ferme, e un invio che fallisce toglie le connessioni di chi se n'è andato.
 */
@Component
@RequiredArgsConstructor
public class NotificationHeartbeatJob {

	private final NotificationStreamService streamService;

	@Scheduled(fixedRate = 30, timeUnit = TimeUnit.SECONDS)
	public void sendHeartbeat() {
		streamService.sendHeartbeat();
	}
}
