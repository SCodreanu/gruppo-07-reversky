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
import it.downfall.reversky.api.catalog.dto.AdminFactionResponse;
import it.downfall.reversky.api.catalog.dto.FactionRequest;
import it.downfall.reversky.api.catalog.dto.FactionTagsRequest;
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

	@Operation(summary = "List the factions", description = "Returns every faction with its tags, sorted by id.")
	@ApiResponse(responseCode = "200", description = "Factions returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@GetMapping
	public ResponseEntity<List<AdminFactionResponse>> findAll() {
		return ResponseEntity.ok(factionService.findAllForAdmin());
	}

	@Operation(summary = "Create a faction", description = "The name must be unique.")
	@ApiResponse(responseCode = "201", description = "Faction created")
	@ApiResponse(responseCode = "400", description = "Missing name or emblem path, or name too long")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "409", description = "Name already used by another faction")
	@PostMapping
	public ResponseEntity<AdminFactionResponse> create(@Valid @RequestBody FactionRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(factionService.create(request));
	}

	@Operation(summary = "Update a faction", description = "Replaces name, emblem and description; the tags stay. The name must stay unique.")
	@ApiResponse(responseCode = "200", description = "Faction updated")
	@ApiResponse(responseCode = "400", description = "Missing name or emblem path, or name too long")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "Faction not found")
	@ApiResponse(responseCode = "409", description = "Name already used by another faction")
	@PutMapping("/{id}")
	public ResponseEntity<AdminFactionResponse> update(@PathVariable Integer id, @Valid @RequestBody FactionRequest request) {
		return ResponseEntity.ok(factionService.update(id, request));
	}

	@Operation(summary = "Replace the tags of a faction", description = "Sets exactly these tags on the faction; an "
		+ "empty list removes them all and a repeated id counts once. Every card of the faction gets them in the tag "
		+ "filters; Spell and Hidden cards have no faction, so faction tags never reach them.")
	@ApiResponse(responseCode = "200", description = "Tags replaced, faction returned with its tags")
	@ApiResponse(responseCode = "400", description = "Missing tag list or a null id")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "Faction not found")
	@ApiResponse(responseCode = "422", description = "A tag does not exist (FACTION_TAG_INVALID): nothing changes")
	@PutMapping("/{id}/tags")
	public ResponseEntity<AdminFactionResponse> replaceTags(@PathVariable Integer id,
			@Valid @RequestBody FactionTagsRequest request) {
		return ResponseEntity.ok(factionService.replaceTags(id, request));
	}
}
