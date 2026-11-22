package it.downfall.reversky.api.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Quante notifiche non lette ha l'utente (GET /api/v1/notifications/unread-count).
 */
public record UnreadCountResponse(

	@Schema(example = "3")
	long count
) {
}
