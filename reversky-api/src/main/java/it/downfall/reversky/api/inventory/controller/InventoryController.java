package it.downfall.reversky.api.inventory.controller;

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
import it.downfall.reversky.api.common.config.OpenApiConfig;
import it.downfall.reversky.api.inventory.dto.InventoryResponse;
import it.downfall.reversky.api.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;

/**
 * Inventario dell'utente autenticato (docs/crediti).
 */
@Tag(name = "Inventory", description = "Credits of the authenticated user")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

	private final InventoryService inventoryService;

	@Operation(summary = "Get my inventory", description = "Returns the credit balance of the user who owns the access token.")
	@ApiResponse(responseCode = "200", description = "Inventory returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "Inventory not found")
	@GetMapping
	public ResponseEntity<InventoryResponse> getInventory(@AuthenticationPrincipal Jwt jwt) {
		Integer userId = Integer.valueOf(jwt.getSubject());
		return ResponseEntity.ok(inventoryService.getInventory(userId));
	}
}
