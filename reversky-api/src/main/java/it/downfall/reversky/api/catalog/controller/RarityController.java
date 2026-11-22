package it.downfall.reversky.api.catalog.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.catalog.dto.RarityResponse;
import it.downfall.reversky.api.catalog.service.RarityService;
import lombok.RequiredArgsConstructor;

/**
 * Elenco pubblico delle rarità, senza token (docs/api-rest, "Carte").
 */
@Tag(name = "Rarities", description = "Rarities of the cards")
@RestController
@RequestMapping("/api/v1/rarities")
@RequiredArgsConstructor
public class RarityController {

	private final RarityService rarityService;

	@Operation(summary = "List the rarities", description = "Returns every rarity, from the most common to the rarest. No token needed.")
	@ApiResponse(responseCode = "200", description = "Rarities returned")
	@GetMapping
	public ResponseEntity<List<RarityResponse>> findAll() {
		return ResponseEntity.ok(rarityService.findAll());
	}
}
