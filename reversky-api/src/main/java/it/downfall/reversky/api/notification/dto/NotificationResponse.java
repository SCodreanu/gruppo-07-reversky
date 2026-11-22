package it.downfall.reversky.api.notification.dto;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.notification.entity.Notification;
import it.downfall.reversky.api.notification.entity.NotificationType;

/**
 * Una notifica (GET /api/v1/notifications e dati degli eventi del canale SSE, docs/notifiche).
 *
 * @param id      anche l'id dell'evento SSE, da rimandare come {@code Last-Event-ID}
 * @param payload id e username delle entità coinvolte; i campi dipendono dal tipo
 */
public record NotificationResponse(

	@Schema(example = "42")
	Integer id,

	@Schema(example = "TRADE_PROPOSED")
	NotificationType type,

	@Schema(example = "{\"tradeId\": \"0b7e2f6a-3c1d-4f8e-9a2b-5d6c7e8f9a0b\", \"userId\": 7, \"username\": \"bob\"}",
		description = "Ids of the entities involved and the other user; the fields depend on the type")
	Map<String, Object> payload,

	@Schema(example = "false")
	boolean read,

	@Schema(example = "2026-11-23T10:00:00Z")
	Instant createdAt
) {

	public static NotificationResponse from(Notification notification) {
		return new NotificationResponse(notification.getId(), notification.getType(), notification.getPayload(),
			notification.isRead(), notification.getCreatedAt());
	}
}
