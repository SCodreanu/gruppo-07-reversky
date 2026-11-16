package it.downfall.reversky.api.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import it.downfall.reversky.api.factionbonus.dto.FactionBonusRequest;
import it.downfall.reversky.api.factionbonus.dto.FactionBonusResponse;
import it.downfall.reversky.api.factionbonus.service.FactionBonusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Matrice dei bonus/malus tra fazioni dal backoffice (docs/backoffice, "Dati di riferimento").
 * <p>
 * Solo ADMIN: lo garantisce SecurityConfig con la regola su /api/v1/admin/**.
 * <p>
 * Il PUT è collection-level: sostituisce l'intera matrice. Il body vuoto {@code []} la svuota.
 */
@Tag(name = "Admin: faction bonuses", description = "Backoffice management of the ATK/DEF bonuses between factions")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin/faction-bonuses")
@RequiredArgsConstructor
public class AdminFactionBonusController {

	private final FactionBonusService factionBonusService;

	@Operation(summary = "List the faction bonuses",
		description = "Returns the full matrix with both faction names, sorted by origin then target id.")
	@ApiResponse(responseCode = "200", description = "Faction bonuses returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@GetMapping
	public ResponseEntity<List<FactionBonusResponse>> findAll() {
		return ResponseEntity.ok(factionBonusService.findAll());
	}

	@Operation(summary = "Replace the faction bonuses",
		description = "Replaces the entire matrix in a single transaction. "
			+ "Each row needs existing factions, origin != target, DEF bonus >= 0. "
			+ "ATK bonus can be negative. An empty body clears the matrix.")
	@ApiResponse(responseCode = "200", description = "Faction bonuses replaced")
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "422", description = "Rule violation: self-matchup, unknown faction, duplicate pair, negative DEF bonus")
	@PutMapping
	public ResponseEntity<List<FactionBonusResponse>> replaceAll(@Valid @RequestBody List<FactionBonusRequest> requests) {
		return ResponseEntity.ok(factionBonusService.replaceAll(requests));
	}
}