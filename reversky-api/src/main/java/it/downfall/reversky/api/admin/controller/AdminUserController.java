package it.downfall.reversky.api.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import it.downfall.reversky.api.inventory.dto.CreditAdjustmentRequest;
import it.downfall.reversky.api.inventory.dto.InventoryResponse;
import it.downfall.reversky.api.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestione degli utenti dal backoffice (docs/backoffice, "Utenti").
 * <p>
 * Solo ADMIN: lo garantisce SecurityConfig con la regola su /api/v1/admin/**.
 */
@Tag(name = "Admin: users", description = "Backoffice management of the users")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

	private final InventoryService inventoryService;

	@Operation(summary = "Adjust the credits of a user", description = "Adds (positive amount) or removes (negative amount) "
		+ "credits with a mandatory reason. The balance never goes below zero.")
	@ApiResponse(responseCode = "200", description = "Credits adjusted, new balance returned")
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields, or amount equal to zero")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "User not found")
	@ApiResponse(responseCode = "422", description = "The balance would go below zero")
	@PostMapping("/{id}/credits")
	public ResponseEntity<InventoryResponse> adjustCredits(@PathVariable Integer id,
			@Valid @RequestBody CreditAdjustmentRequest request) {
		return ResponseEntity.ok(inventoryService.adjustCredits(id, request.amount(), request.reason()));
	}
}
