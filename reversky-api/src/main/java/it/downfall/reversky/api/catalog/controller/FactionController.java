package it.downfall.reversky.api.catalog.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.catalog.dto.FactionResponse;
import it.downfall.reversky.api.catalog.service.FactionService;
import lombok.RequiredArgsConstructor;

/**
 * Elenco pubblico delle fazioni, senza token (docs/api-rest, "Carte").
 */
@Tag(name = "Factions", description = "Factions of the game")
@RestController
@RequestMapping("/api/v1/factions")
@RequiredArgsConstructor
public class FactionController {

	private final FactionService factionService;

	@Operation(summary = "List the factions", description = "Returns every faction, sorted by id. No token needed.")
	@ApiResponse(responseCode = "200", description = "Factions returned")
	@GetMapping
	public ResponseEntity<List<FactionResponse>> findAll() {
		return ResponseEntity.ok(factionService.findAll());
	}
}
