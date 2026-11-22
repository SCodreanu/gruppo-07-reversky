package it.downfall.reversky.api.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.catalog.dto.FactionRequest;
import it.downfall.reversky.api.catalog.dto.FactionResponse;
import it.downfall.reversky.api.catalog.service.FactionService;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestione delle fazioni dal backoffice (docs/backoffice, "Dati di riferimento").
 * <p>
 * Solo ADMIN: lo garantisce SecurityConfig con la regola su /api/v1/admin/**.
 */
@Tag(name = "Admin: factions", description = "Backoffice management of the factions")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin/factions")
@RequiredArgsConstructor
public class AdminFactionController {

	private final FactionService factionService;

	@Operation(summary = "List the factions", description = "Returns every faction, sorted by id.")
	@ApiResponse(responseCode = "200", description = "Factions returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@GetMapping
	public ResponseEntity<List<FactionResponse>> findAll() {
		return ResponseEntity.ok(factionService.findAll().stream().toList());
	}

	@Operation(summary = "Create a faction", description = "The name must be unique.")
	@ApiResponse(responseCode = "201", description = "Faction created")
	@ApiResponse(responseCode = "400", description = "Missing name or emblem path, or name too long")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "409", description = "Name already used by another faction")
	@PostMapping
	public ResponseEntity<FactionResponse> create(@Valid @RequestBody FactionRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(factionService.create(request));
	}

	@Operation(summary = "Update a faction", description = "Replaces name, emblem and description. The name must stay unique.")
	@ApiResponse(responseCode = "200", description = "Faction updated")
	@ApiResponse(responseCode = "400", description = "Missing name or emblem path, or name too long")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "Faction not found")
	@ApiResponse(responseCode = "409", description = "Name already used by another faction")
	@PutMapping("/{id}")
	public ResponseEntity<FactionResponse> update(@PathVariable Integer id, @Valid @RequestBody FactionRequest request) {
		return ResponseEntity.ok(factionService.update(id, request));
	}
}
