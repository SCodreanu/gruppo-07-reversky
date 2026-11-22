package it.downfall.reversky.api.battle.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.battle.dto.QueueJoinRequest;
import it.downfall.reversky.api.battle.dto.QueueTicketResponse;
import it.downfall.reversky.api.battle.service.MatchmakingQueueService;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Coda classificata dell'utente autenticato (docs/sfide, "Classificata: matchmaking").
 */
@Tag(name = "Matchmaking", description = "Ranked queue of the authenticated user")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/matchmaking/queue")
@RequiredArgsConstructor
public class MatchmakingController {

	private final MatchmakingQueueService queueService;

	@Operation(summary = "Join the ranked queue", description = "Queues me for a ranked battle with one of my decks, "
		+ "which must be valid now. The ticket keeps my current rating; my first time also creates my rating (1000).")
	@ApiResponse(responseCode = "201", description = "In the queue, ticket returned")
	@ApiResponse(responseCode = "400", description = "Missing deck id")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "Deck not found")
	@ApiResponse(responseCode = "409", description = "Already in the queue (ALREADY_IN_QUEUE)")
	@ApiResponse(responseCode = "422", description = "Invalid deck (DECK_INVALID with violations)")
	@PostMapping
	public ResponseEntity<QueueTicketResponse> join(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody QueueJoinRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(queueService.join(userId(jwt), request));
	}

	@Operation(summary = "Get my queue ticket", description = "Returns my ticket with the current search window. "
		+ "404 means I am no longer in the queue: matched, left, or removed because the deck changed.")
	@ApiResponse(responseCode = "200", description = "Ticket returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "Not in the queue (QUEUE_TICKET_NOT_FOUND)")
	@GetMapping
	public ResponseEntity<QueueTicketResponse> getMine(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(queueService.getMine(userId(jwt)));
	}

	@Operation(summary = "Leave the ranked queue", description = "Removes my ticket. Also 204 if I was not in the "
		+ "queue, for example because I was just matched.")
	@ApiResponse(responseCode = "204", description = "Not in the queue any more")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@DeleteMapping
	public ResponseEntity<Void> leave(@AuthenticationPrincipal Jwt jwt) {
		queueService.leave(userId(jwt));
		return ResponseEntity.noContent().build();
	}

	private static Integer userId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}
}
