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
import it.downfall.reversky.api.catalog.dto.RarityCreateRequest;
import it.downfall.reversky.api.catalog.dto.RarityResponse;
import it.downfall.reversky.api.catalog.dto.RarityUpdateRequest;
import it.downfall.reversky.api.catalog.service.RarityService;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestione delle rarità dal backoffice (docs/backoffice, "Dati di riferimento").
 * <p>
 * Solo ADMIN: lo garantisce SecurityConfig con la regola su /api/v1/admin/**.
 */
@Tag(name = "Admin: rarities", description = "Backoffice management of the rarities")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin/rarities")
@RequiredArgsConstructor
public class AdminRarityController {

	private final RarityService rarityService;

	@Operation(summary = "List the rarities", description = "Returns every rarity, from the most common to the rarest.")
	@ApiResponse(responseCode = "200", description = "Rarities returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@GetMapping
	public ResponseEntity<List<RarityResponse>> findAll() {
		return ResponseEntity.ok(rarityService.findAll());
	}

	@Operation(summary = "Create a rarity", description = "Name and sorting order must be unique. The sorting order cannot be changed later: the engine receives it as rarityOrder.")
	@ApiResponse(responseCode = "201", description = "Rarity created")
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "409", description = "Name or sorting order already used by another rarity")
	@PostMapping
	public ResponseEntity<RarityResponse> create(@Valid @RequestBody RarityCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(rarityService.create(request));
	}

	@Operation(summary = "Update a rarity", description = "Replaces name and limits. The sorting order is not part of the request and never changes.")
	@ApiResponse(responseCode = "200", description = "Rarity updated")
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "Rarity not found")
	@ApiResponse(responseCode = "409", description = "Name already used by another rarity")
	@PutMapping("/{id}")
	public ResponseEntity<RarityResponse> update(@PathVariable Integer id, @Valid @RequestBody RarityUpdateRequest request) {
		return ResponseEntity.ok(rarityService.update(id, request));
	}
}
