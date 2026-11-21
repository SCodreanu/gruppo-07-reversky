package it.downfall.reversky.api.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.catalog.dto.EffectResponse;
import it.downfall.reversky.api.catalog.service.EffectService;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import lombok.RequiredArgsConstructor;

/**
 * Elenco degli effetti assegnabili alle carte, per il backoffice (docs/backoffice, regola 2, API-36).
 * <p>
 * Solo lettura e solo ADMIN (regola su /api/v1/admin/** di SecurityConfig): un effetto nuovo arriva con una
 * migrazione, insieme al suo codice nell'engine.
 */
@Tag(name = "Admin: effects", description = "Effects that can be assigned to cards")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin/effects")
@RequiredArgsConstructor
public class AdminEffectController {

	private final EffectService effectService;

	@Operation(summary = "List the effects", description = "Every effect code implemented by the game engine, with the "
			+ "only card type that can carry it, sorted by card type and then by code. Read only: new effects arrive "
			+ "with the engine code that implements them.")
	@ApiResponse(responseCode = "200", description = "Effects returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@GetMapping
	public ResponseEntity<List<EffectResponse>> findAll() {
		return ResponseEntity.ok(effectService.findAll());
	}
}
