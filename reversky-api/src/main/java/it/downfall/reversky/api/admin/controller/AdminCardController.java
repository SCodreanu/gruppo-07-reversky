package it.downfall.reversky.api.admin.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.catalog.dto.CatalogCardResponse;
import it.downfall.reversky.api.catalog.dto.CatalogCardUpdateRequest;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.service.CatalogCardService;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import it.downfall.reversky.api.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestione delle carte dal backoffice (docs/backoffice, "Carte").
 * <p>
 * Solo ADMIN: lo garantisce SecurityConfig con la regola su /api/v1/admin/**.
 * POST /admin/cards (creazione con l'illustrazione) arriva con API-14: fino ad allora le carte
 * si inseriscono da migrazione.
 */
@Tag(name = "Admin: cards", description = "Backoffice management of the card catalog")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin/cards")
@RequiredArgsConstructor
public class AdminCardController {

	private final CatalogCardService catalogCardService;

	// Pageable nascosto in Swagger come in CardController: l'ordine è fisso, si documentano solo page e size
	@Operation(summary = "List the cards", description = "Returns every card, deactivated ones included, sorted by name.")
	@Parameter(name = "page", in = ParameterIn.QUERY, description = "Page number, from 0", schema = @Schema(type = "integer", defaultValue = "0"))
	@Parameter(name = "size", in = ParameterIn.QUERY, description = "Cards per page, at most 100", schema = @Schema(type = "integer", defaultValue = "20"))
	@ApiResponse(responseCode = "200", description = "Page of cards returned")
	@ApiResponse(responseCode = "400", description = "Unknown card type")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@GetMapping
	public ResponseEntity<PageResponse<CatalogCardResponse>> findAll(@Parameter(description = "Only cards of this type") @RequestParam(required = false) CardType type, @Parameter(description = "Only cards whose name contains this text, ignoring case", example = "wend") @RequestParam(required = false) String name, @Parameter(description = "true = only active cards, false = only deactivated cards, empty = all") @RequestParam(required = false) Boolean active, @Parameter(hidden = true) Pageable pageable) {
		return ResponseEntity.ok(catalogCardService.findAll(type, name, active, pageable));
	}

	@Operation(summary = "Update a card", description = "Replaces the fields of a card. The image is not part of the request. " + "For a variant, the game fields of the request (type, subtype, effect, stats, copy limit) are ignored and copied from the original. " + "Updating an original also updates the game fields of all its variants. " + "Set variantOfId to make the card a variant of an original card, or leave it empty to make it an original.")
	@ApiResponse(responseCode = "200", description = "Card updated")
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "Card not found")
	@ApiResponse(responseCode = "409", description = "Name already used by another original card")
	@ApiResponse(responseCode = "422", description = "Stats, subtype or effect do not match the card type, or variantOfId does not point to an original card")
	@PutMapping("/{id}")
	public ResponseEntity<CatalogCardResponse> update(@PathVariable Integer id, @Valid @RequestBody CatalogCardUpdateRequest request) {
		return ResponseEntity.ok(catalogCardService.update(id, request));
	}

	@Operation(summary = "Deactivate a card", description = "The card no longer drops from packs but stays in collections. Cards are never deleted. " + "Its variants are not changed. Deactivating a card that is already inactive changes nothing.")
	@ApiResponse(responseCode = "200", description = "Card deactivated")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "Card not found")
	@PostMapping("/{id}/deactivate")
	public ResponseEntity<CatalogCardResponse> deactivate(@PathVariable Integer id) {
		return ResponseEntity.ok(catalogCardService.deactivate(id));
	}

	@Operation(summary = "Activate a card", description = "Puts the card back in the packs. Activating a card that is already active changes nothing.")
	@ApiResponse(responseCode = "200", description = "Card activated")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "Card not found")
	@PostMapping("/{id}/activate")
	public ResponseEntity<CatalogCardResponse> activate(@PathVariable Integer id) {
		return ResponseEntity.ok(catalogCardService.activate(id));
	}
}
