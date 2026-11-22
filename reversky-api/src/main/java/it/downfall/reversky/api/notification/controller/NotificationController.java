package it.downfall.reversky.api.notification.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import it.downfall.reversky.api.common.dto.PageResponse;
import it.downfall.reversky.api.notification.dto.NotificationResponse;
import it.downfall.reversky.api.notification.dto.UnreadCountResponse;
import it.downfall.reversky.api.notification.service.NotificationService;
import it.downfall.reversky.api.notification.service.NotificationStreamService;
import lombok.RequiredArgsConstructor;

/**
 * Notifiche dell'utente autenticato (docs/notifiche, API-65 e API-66): lista, contatore delle non lette, lettura e
 * canale SSE.
 */
@Tag(name = "Notifications", description = "Notifications of the authenticated user")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;
	private final NotificationStreamService streamService;

	// Pageable nascosto in Swagger come nelle altre liste: l'ordine è fisso, dalla notifica più recente
	@Operation(summary = "List my notifications", description = "Returns my notifications, newest first. "
		+ "With unread=true only the ones I have not read yet.")
	@Parameter(name = "page", in = ParameterIn.QUERY, description = "Page number, from 0", schema = @Schema(type = "integer", defaultValue = "0"))
	@Parameter(name = "size", in = ParameterIn.QUERY, description = "Notifications per page, at most 100", schema = @Schema(type = "integer", defaultValue = "20"))
	@ApiResponse(responseCode = "200", description = "Page of notifications returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@GetMapping
	public ResponseEntity<PageResponse<NotificationResponse>> findMine(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Only unread notifications") @RequestParam(defaultValue = "false") boolean unread,
			@Parameter(hidden = true) Pageable pageable) {
		return ResponseEntity.ok(notificationService.findMine(userId(jwt), unread, pageable));
	}

	@Operation(summary = "Count my unread notifications")
	@ApiResponse(responseCode = "200", description = "Number of unread notifications returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@GetMapping("/unread-count")
	public ResponseEntity<UnreadCountResponse> countUnread(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(notificationService.countUnread(userId(jwt)));
	}

	@Operation(summary = "Mark one of my notifications as read", description = "Also 204 if it was already read.")
	@ApiResponse(responseCode = "204", description = "Notification read")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "Notification not found, or not mine (NOTIFICATION_NOT_FOUND)")
	@PostMapping("/{id}/read")
	public ResponseEntity<Void> markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
		notificationService.markRead(userId(jwt), id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Mark all my notifications as read")
	@ApiResponse(responseCode = "204", description = "All my notifications read")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@PostMapping("/read-all")
	public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Jwt jwt) {
		notificationService.markAllRead(userId(jwt));
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Open my notification stream", description = "Server-Sent Events stream of my new notifications. "
		+ "Send the access token in the Authorization header, as for every other call: the browser's native "
		+ "EventSource cannot, so use a fetch-based client. Each event has id = notification id, event = notification "
		+ "type and data = the notification JSON; a ': ping' comment comes every 30 seconds. The stream closes when the "
		+ "access token expires: reconnect with a new token and Last-Event-ID to get the notifications created in "
		+ "between (at most the 100 newest, the others are in GET /notifications). An id seen twice can be ignored.")
	@ApiResponse(responseCode = "200", description = "Stream open (text/event-stream)")
	@ApiResponse(responseCode = "400", description = "Last-Event-ID is not a notification id")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "My account is disabled (AUTH_ACCOUNT_DISABLED)")
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Id of the last notification received, to get the ones created after it")
			@RequestHeader(name = "Last-Event-ID", required = false) Integer lastEventId) {
		return streamService.open(userId(jwt), lastEventId, jwt.getExpiresAt());
	}

	private static Integer userId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}
}
