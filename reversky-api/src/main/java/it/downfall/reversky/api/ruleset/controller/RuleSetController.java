package it.downfall.reversky.api.ruleset.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.ruleset.dto.RuleSetPublicResponse;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import lombok.RequiredArgsConstructor;

/**
 * Regole di gioco attive, per i client (docs/backoffice, "Regole di gioco").
 * <p>
 * Endpoint pubblico: SecurityConfig non richiede token su GET /rules.
 */
@Tag(name = "Rules", description = "Game rules currently in effect")
@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class RuleSetController {

	private final RuleSetService ruleSetService;

	@Operation(summary = "Get the active rule set",
		description = "Returns starting life points, max rounds, deck composition and credit rewards in effect.")
	@ApiResponse(responseCode = "200", description = "Active rule set returned")
	@ApiResponse(responseCode = "404", description = "No active rule set")
	@GetMapping
	public ResponseEntity<RuleSetPublicResponse> findActive() {
		return ResponseEntity.ok(ruleSetService.findActive());
	}
}