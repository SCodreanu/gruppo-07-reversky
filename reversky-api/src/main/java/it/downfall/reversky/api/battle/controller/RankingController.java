package it.downfall.reversky.api.battle.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.battle.dto.RankingResponse;
import it.downfall.reversky.api.battle.service.RankingService;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import lombok.RequiredArgsConstructor;

/**
 * Rating Elo dell'utente autenticato (docs/sfide). Non c'è una classifica pubblica (Q-10).
 */
@Tag(name = "Rankings", description = "Elo rating and ranked statistics of the authenticated user")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
public class RankingController {

	private final RankingService rankingService;

	@Operation(summary = "Get my rating", description = "Returns my Elo rating, my peak rating and my ranked games, "
		+ "wins, losses and draws. Before my first ranked queue I get the initial rating (1000) and zero games.")
	@ApiResponse(responseCode = "200", description = "Rating returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@GetMapping("/me")
	public ResponseEntity<RankingResponse> getMine(@AuthenticationPrincipal Jwt jwt) {
		Integer userId = Integer.valueOf(jwt.getSubject());
		return ResponseEntity.ok(rankingService.getMine(userId));
	}
}
