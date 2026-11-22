package it.downfall.reversky.api.trade.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import it.downfall.reversky.api.common.config.OpenApiConfig;
import it.downfall.reversky.api.common.dto.PageResponse;
import it.downfall.reversky.api.trade.dto.TradeResponse;
import it.downfall.reversky.api.trade.service.TradeService;
import lombok.RequiredArgsConstructor;

/**
 * Scambi di carte tra utenti (docs/scambi). L'utente si ricava sempre dal token.
 */
@Tag(name = "Trades", description = "Card trades between players")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/trades")
@RequiredArgsConstructor
public class TradeController {

	private final TradeService tradeService;

	// Pageable nascosto in Swagger: l'ordine è fisso, si documentano solo page e size
	@Operation(summary = "List my open trades", description = "incoming: open trades where it is my turn to answer; "
		+ "outgoing: open trades waiting for the other player. Latest move first. Closed trades are read with "
		+ "GET /trades/{id}.")
	@Parameter(name = "page", in = ParameterIn.QUERY, description = "Page number, from 0", schema = @Schema(type = "integer", defaultValue = "0"))
	@Parameter(name = "size", in = ParameterIn.QUERY, description = "Trades per page, at most 100", schema = @Schema(type = "integer", defaultValue = "20"))
	@ApiResponse(responseCode = "200", description = "Page of trades returned")
	@ApiResponse(responseCode = "400", description = "box is not incoming or outgoing (INVALID_TRADE_BOX)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@GetMapping
	public ResponseEntity<PageResponse<TradeResponse>> findBox(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "incoming or outgoing", example = "incoming") @RequestParam String box,
			@Parameter(hidden = true) Pageable pageable) {
		return ResponseEntity.ok(tradeService.findBox(userId(jwt), box, pageable));
	}

	@Operation(summary = "Get a trade", description = "A trade I take part in, open or closed, from my point of view.")
	@ApiResponse(responseCode = "200", description = "Trade returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "Trade not found or not mine (TRADE_NOT_FOUND)")
	@GetMapping("/{id}")
	public ResponseEntity<TradeResponse> findById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return ResponseEntity.ok(tradeService.findById(userId(jwt), id));
	}

	@Operation(summary = "Reject or withdraw a trade", description = "Closes an open trade without exchanging "
		+ "anything. Either player can do it at any time, even when it is not their turn; the reserved copies are "
		+ "available again right away.")
	@ApiResponse(responseCode = "200", description = "Trade rejected, returned with status REJECTED")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "Trade not found or not mine (TRADE_NOT_FOUND)")
	@ApiResponse(responseCode = "409", description = "Trade already accepted or rejected (TRADE_CLOSED)")
	@PostMapping("/{id}/reject")
	public ResponseEntity<TradeResponse> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return ResponseEntity.ok(tradeService.reject(userId(jwt), id));
	}

	private static Integer userId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}
}
