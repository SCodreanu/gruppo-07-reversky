package it.downfall.reversky.api.battle.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.battle.dto.BattleDetailResponse;
import it.downfall.reversky.api.battle.dto.BattleSummaryResponse;
import it.downfall.reversky.api.battle.entity.BattleMode;
import it.downfall.reversky.api.battle.service.BattleService;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import it.downfall.reversky.api.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;

/**
 * Storico e dettaglio delle partite (docs/sfide, "Storico"). Solo i due giocatori vedono una partita.
 */
@Tag(name = "Battles", description = "Battles I played")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/battles")
@RequiredArgsConstructor
public class BattleController {

	private final BattleService battleService;

	// Pageable nascosto in Swagger come nelle altre liste: l'ordine è fisso, dalla partita più recente
	@Operation(summary = "List my battles", description = "Returns the battles I played, newest first, without the log.")
	@Parameter(name = "page", in = ParameterIn.QUERY, description = "Page number, from 0", schema = @Schema(type = "integer", defaultValue = "0"))
	@Parameter(name = "size", in = ParameterIn.QUERY, description = "Battles per page, at most 100", schema = @Schema(type = "integer", defaultValue = "20"))
	@ApiResponse(responseCode = "200", description = "Page of battles returned")
	@ApiResponse(responseCode = "400", description = "Unknown mode")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@GetMapping
	public ResponseEntity<PageResponse<BattleSummaryResponse>> findHistory(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Only battles of this mode") @RequestParam(required = false) BattleMode mode,
			@Parameter(hidden = true) Pageable pageable) {
		return ResponseEntity.ok(battleService.findHistory(userId(jwt), mode, pageable));
	}

	@Operation(summary = "Get one of my battles", description = "Returns the battle from my point of view, with both decks "
		+ "as they were and the round by round log.")
	@ApiResponse(responseCode = "200", description = "Battle returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "Battle not found, or not played by me")
	@GetMapping("/{id}")
	public ResponseEntity<BattleDetailResponse> findById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return ResponseEntity.ok(battleService.findMine(userId(jwt), id));
	}

	private static Integer userId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}
}
