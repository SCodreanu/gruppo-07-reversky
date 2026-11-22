package it.downfall.reversky.api.battle.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.battle.dto.BattleAcceptRequest;
import it.downfall.reversky.api.battle.dto.BattleDetailResponse;
import it.downfall.reversky.api.battle.dto.BattleInviteRequest;
import it.downfall.reversky.api.battle.dto.BattleInviteResponse;
import it.downfall.reversky.api.battle.service.BattleInviteService;
import it.downfall.reversky.api.battle.service.BattleService;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Inviti alle sfide amichevoli (docs/sfide). L'utente si ricava sempre dal token.
 */
@Tag(name = "Battle invites", description = "Friendly challenges between friends")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/battle-invites")
@RequiredArgsConstructor
public class BattleInviteController {

	private final BattleInviteService battleInviteService;
	private final BattleService battleService;

	@Operation(summary = "Challenge a friend", description = "Invites a friend to a friendly battle with one of my "
		+ "decks, which must be valid now.")
	@ApiResponse(responseCode = "201", description = "Challenge sent")
	@ApiResponse(responseCode = "400", description = "Missing fields")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "The opponent is not a friend")
	@ApiResponse(responseCode = "404", description = "Opponent or deck not found")
	@ApiResponse(responseCode = "409", description = "A challenge to this player is already pending")
	@ApiResponse(responseCode = "422", description = "Challenging myself, or an invalid deck (DECK_INVALID with violations)")
	@PostMapping
	public ResponseEntity<BattleInviteResponse> create(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody BattleInviteRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(battleInviteService.create(userId(jwt), request));
	}

	@Operation(summary = "List my pending challenges", description = "incoming: challenges I received; outgoing: "
		+ "challenges I sent, with my deck. Newest first.")
	@ApiResponse(responseCode = "200", description = "Challenges returned")
	@ApiResponse(responseCode = "400", description = "box is not incoming or outgoing")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@GetMapping
	public ResponseEntity<List<BattleInviteResponse>> findBox(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "incoming or outgoing", example = "incoming") @RequestParam String box) {
		return ResponseEntity.ok(battleInviteService.findBox(userId(jwt), box));
	}

	@Operation(summary = "Accept a challenge", description = "Accepts a challenge I received with one of my decks. Both "
		+ "decks are validated again, the server generates the seed and the engine simulates the battle right away. "
		+ "If the engine is unavailable the answer is 503 with battleId: the battle is saved and POST /battles/{id}/retry "
		+ "completes it.")
	@ApiResponse(responseCode = "200", description = "Battle simulated, returned from my point of view")
	@ApiResponse(responseCode = "400", description = "Missing deck id")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "We are no longer friends")
	@ApiResponse(responseCode = "404", description = "Challenge not found or not for me, or deck not found")
	@ApiResponse(responseCode = "409", description = "Challenge already accepted")
	@ApiResponse(responseCode = "422", description = "One of the two decks is no longer valid: the challenge stays pending")
	@ApiResponse(responseCode = "500", description = "ENGINE_INPUT_REJECTED: the engine rejected the battle data; the battle stays without a result (battleId)")
	@ApiResponse(responseCode = "503", description = "Engine unavailable: the battle waits for a retry (battleId)")
	@PostMapping("/{id}/accept")
	public ResponseEntity<BattleDetailResponse> accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@Valid @RequestBody BattleAcceptRequest request) {
		return ResponseEntity.ok(battleService.accept(userId(jwt), id, request.deckId()));
	}

	@Operation(summary = "Reject a challenge", description = "Deletes a pending challenge I received.")
	@ApiResponse(responseCode = "204", description = "Challenge rejected")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "Challenge not found, not for me or no longer pending")
	@PostMapping("/{id}/reject")
	public ResponseEntity<Void> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		battleInviteService.reject(userId(jwt), id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Withdraw a challenge", description = "Deletes a pending challenge I sent.")
	@ApiResponse(responseCode = "204", description = "Challenge withdrawn")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "Challenge not found, not mine or no longer pending")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		battleInviteService.withdraw(userId(jwt), id);
		return ResponseEntity.noContent().build();
	}

	private static Integer userId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}
}
