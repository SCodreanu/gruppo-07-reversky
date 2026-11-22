package it.downfall.reversky.api.catalog.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.catalog.dto.CatalogCardResponse;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.service.CatalogCardService;
import it.downfall.reversky.api.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;

/**
 * Catalogo pubblico delle carte, senza token (docs/api-rest, "Carte"): serve anche alle pagine SEO.
 * <p>
 * L'accesso pubblico lo dà SecurityConfig con la regola GET /api/v1/cards/**.
 */
@Tag(name = "Cards", description = "Public card catalog")
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

	private final CatalogCardService catalogCardService;

	// Pageable è nascosto in Swagger, che altrimenti mostrerebbe anche "sort": l'ordine è fisso
	// (per nome) e il service ignora quello del client. Al suo posto si documentano page e size.
	@Operation(summary = "List the cards", description = "Returns the active cards, variants included, sorted by name. No token needed. " + "Deactivated cards are not listed, but they can still be read by id.")
	@Parameter(name = "page", in = ParameterIn.QUERY, description = "Page number, from 0", schema = @Schema(type = "integer", defaultValue = "0"))
	@Parameter(name = "size", in = ParameterIn.QUERY, description = "Cards per page, at most 100", schema = @Schema(type = "integer", defaultValue = "20"))
	@ApiResponse(responseCode = "200", description = "Page of cards returned")
	@ApiResponse(responseCode = "400", description = "Unknown card type")
	@GetMapping
	public ResponseEntity<PageResponse<CatalogCardResponse>> findAll(@Parameter(description = "Only cards of this type") @RequestParam(required = false) CardType type, @Parameter(description = "Only cards whose name contains this text, ignoring case", example = "wend") @RequestParam(required = false) String name, @Parameter(description = "Only cards of this rarity (id from GET /rarities). An unknown id returns an empty page", example = "3") @RequestParam(required = false) Integer rarityId, @Parameter(hidden = true) Pageable pageable) {
		return ResponseEntity.ok(catalogCardService.findActive(type, name, rarityId, pageable));
	}

	@Operation(summary = "Get a card", description = "Returns one card, even if it is deactivated: collections and battle history still use it. No token needed.")
	@ApiResponse(responseCode = "200", description = "Card returned")
	@ApiResponse(responseCode = "404", description = "Card not found")
	@GetMapping("/{id}")
	public ResponseEntity<CatalogCardResponse> findById(@PathVariable Integer id) {
		return ResponseEntity.ok(catalogCardService.findById(id));
	}
}
