package it.downfall.reversky.api.notification.service;

import it.downfall.reversky.api.notification.dto.NotificationResponse;

/**
 * Una notifica è stata salvata (API-66). Lo pubblica {@link NotificationService#notify} dentro la transazione del
 * chiamante; {@link NotificationStreamService} lo riceve solo dopo il commit e invia la notifica sul canale SSE, così
 * una transazione annullata non invia niente.
 *
 * @param userId       destinatario
 * @param notification la notifica salvata, con il suo id
 */
public record NotificationCreatedEvent(Integer userId, NotificationResponse notification) {
}
