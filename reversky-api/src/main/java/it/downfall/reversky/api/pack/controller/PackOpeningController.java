package it.downfall.reversky.api.pack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import it.downfall.reversky.api.pack.dto.OpenPackResponse;
import it.downfall.reversky.api.pack.service.PackService;
import lombok.RequiredArgsConstructor;

/**
 * Apertura dei pacchetti dell'inventario (docs/pacchetti, "Apertura").
 */
@Tag(name = "Inventory", description = "Credits of the authenticated user")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/inventory/packs")
@RequiredArgsConstructor
public class PackOpeningController {

	private final PackService packService;

	@Operation(summary = "Open a pack", description = "Opens one of my packs of this type: the cards are drawn on the "
		+ "server and added to my collection. The answer lists them in slot order, with isNew.")
	@ApiResponse(responseCode = "200", description = "Pack opened")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "422", description = "No pack of this type to open, or a slot without cards (the pack stays)")
	@PostMapping("/{packId}/open")
	public ResponseEntity<OpenPackResponse> open(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer packId) {
		return ResponseEntity.ok(packService.open(Integer.valueOf(jwt.getSubject()), packId));
	}
}
