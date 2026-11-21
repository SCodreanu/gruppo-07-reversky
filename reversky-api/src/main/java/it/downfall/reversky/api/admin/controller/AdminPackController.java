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
import it.downfall.reversky.api.common.config.OpenApiConfig;
import it.downfall.reversky.api.pack.dto.AdminPackResponse;
import it.downfall.reversky.api.pack.dto.PackRequest;
import it.downfall.reversky.api.pack.service.PackAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestione dei pacchetti dal backoffice (docs/backoffice, "Pacchetti").
 * <p>
 * Solo ADMIN: lo garantisce SecurityConfig con la regola su /api/v1/admin/**.
 */
@Tag(name = "Admin: packs", description = "Backoffice management of the packs")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin/packs")
@RequiredArgsConstructor
public class AdminPackController {

	private final PackAdminService packAdminService;

	@Operation(summary = "List the packs", description = "Returns every pack, deactivated ones included, sorted by id.")
	@ApiResponse(responseCode = "200", description = "Packs returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@GetMapping
	public ResponseEntity<List<AdminPackResponse>> findAll() {
		return ResponseEntity.ok(packAdminService.findAll());
	}

	@Operation(summary = "Create a pack", description = "Creates a deactivated pack with its base slots and bonus odds. "
		+ "The name must be unique; a free pack (cost 0) needs a purchase limit; every bonus must be rarer than all the "
		+ "base slots.")
	@ApiResponse(responseCode = "201", description = "Pack created, deactivated")
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "409", description = "Name already used by another pack")
	@ApiResponse(responseCode = "422", description = "Unknown rarity, or a bonus not rarer than every base slot")
	@PostMapping
	public ResponseEntity<AdminPackResponse> create(@Valid @RequestBody PackRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(packAdminService.create(request));
	}

	@Operation(summary = "Update a pack", description = "Replaces every field, slots and bonuses included. An active pack "
		+ "must still be able to fill every base slot.")
	@ApiResponse(responseCode = "200", description = "Pack updated")
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "Pack not found")
	@ApiResponse(responseCode = "409", description = "Name already used by another pack")
	@ApiResponse(responseCode = "422", description = "Unknown rarity, bonus not rarer than every slot, or an active pack "
		+ "that could no longer be filled")
	@PutMapping("/{id}")
	public ResponseEntity<AdminPackResponse> update(@PathVariable Integer id, @Valid @RequestBody PackRequest request) {
		return ResponseEntity.ok(packAdminService.update(id, request));
	}

	@Operation(summary = "Activate a pack", description = "Puts the pack on sale, if every base slot can find a card.")
	@ApiResponse(responseCode = "200", description = "Pack activated")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "Pack not found")
	@ApiResponse(responseCode = "422", description = "A base slot could not find any card, not even with the fallback")
	@PostMapping("/{id}/activate")
	public ResponseEntity<AdminPackResponse> activate(@PathVariable Integer id) {
		return ResponseEntity.ok(packAdminService.activate(id));
	}

	@Operation(summary = "Deactivate a pack", description = "Removes the pack from the shop. Packs already bought can "
		+ "still be opened.")
	@ApiResponse(responseCode = "200", description = "Pack deactivated")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "Pack not found")
	@PostMapping("/{id}/deactivate")
	public ResponseEntity<AdminPackResponse> deactivate(@PathVariable Integer id) {
		return ResponseEntity.ok(packAdminService.deactivate(id));
	}
}
