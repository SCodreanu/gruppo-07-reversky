package it.downfall.reversky.api.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.catalog.dto.FactionBonusRequest;
import it.downfall.reversky.api.catalog.dto.FactionBonusResponse;
import it.downfall.reversky.api.catalog.service.FactionBonusService;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

/**
 * Matrice dei bonus/malus tra fazioni dal backoffice (docs/backoffice, "Dati di riferimento").
 * <p>
 * Solo ADMIN: lo garantisce SecurityConfig con la regola su /api/v1/admin/**.
 * <p>
 * Il PUT è collection-level: sostituisce l'intera matrice. Il body vuoto {@code []} la svuota.
 * <p>
 * Il corpo è una lista, quindi i vincoli di ogni riga ({@code @NotNull} sulla riga, annotazioni di
 * {@link FactionBonusRequest}) li controlla la validazione dei metodi di Spring: l'errore è una
 * {@code HandlerMethodValidationException}, che GlobalExceptionHandler trasforma nello stesso
 * {@code 400 VALIDATION_FAILED} degli altri DTO, con l'indice della riga nel campo.
 */
@Tag(name = "Admin: faction bonuses", description = "Backoffice management of the ATK/DEF bonuses between factions")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin/faction-bonuses")
@RequiredArgsConstructor
public class AdminFactionBonusController {

	private final FactionBonusService factionBonusService;

	@Operation(summary = "List the faction bonuses",
		description = "Returns the full matrix with both faction names, sorted by origin then target id.")
	@ApiResponse(responseCode = "200", description = "Faction bonuses returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@GetMapping
	public ResponseEntity<List<FactionBonusResponse>> findAll() {
		return ResponseEntity.ok(factionBonusService.findAll());
	}

	@Operation(summary = "Replace the faction bonuses",
		description = "Replaces the entire matrix in a single transaction: pairs missing from the body are deleted. "
			+ "Each row needs two different existing factions and a DEF bonus >= 0; the ATK bonus can be negative (malus). "
			+ "If any row is invalid nothing changes. An empty body [] clears the matrix. "
			+ "Two replacements at the same time are queued and the last one wins. "
			+ "The change applies to every future battle.")
	@ApiResponse(responseCode = "200", description = "Faction bonuses replaced: the new matrix, sorted by origin then target id")
	@ApiResponse(responseCode = "400", description = "Null row, missing field or negative DEF bonus (VALIDATION_FAILED, "
		+ "with the row index in errors[].field, e.g. [0].defBonus)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "422", description = "Faction matched against itself (FACTION_BONUS_SELF_MATCHUP), "
		+ "unknown faction (FACTION_BONUS_FACTION_NOT_FOUND) or pair repeated in the body (FACTION_BONUS_DUPLICATE)")
	@PutMapping
	public ResponseEntity<List<FactionBonusResponse>> replaceAll(@RequestBody List<@NotNull @Valid FactionBonusRequest> requests) {
		return ResponseEntity.ok(factionBonusService.replaceAll(requests));
	}
}
