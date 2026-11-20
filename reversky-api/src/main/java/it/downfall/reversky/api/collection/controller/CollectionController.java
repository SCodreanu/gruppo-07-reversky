package it.downfall.reversky.api.collection.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.collection.dto.AvailableCardResponse;
import it.downfall.reversky.api.collection.dto.CollectionEntryResponse;
import it.downfall.reversky.api.collection.dto.CollectionFilter;
import it.downfall.reversky.api.collection.dto.CollectionSummaryResponse;
import it.downfall.reversky.api.collection.service.CollectionService;
import it.downfall.reversky.api.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;

/**
 * Collezione (docs/collezione): la mia, il suo completamento e quella di un altro utente. Tutti gli endpoint
 * chiedono un token; l'utente corrente si ricava sempre dal token.
 * <p>
 * Anche GET /users/{id}/collection sta qui, per tenere insieme la funzionalità (deciso il 20 novembre).
 */
@Tag(name = "Collection", description = "Cards owned by users")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CollectionController {

	private final CollectionService collectionService;

	// Pageable è nascosto in Swagger e page, size e sort si documentano a mano, con le proprietà di sort ammesse
	@Operation(summary = "List my collection",
		description = "Returns every card I own, deactivated cards and variants included, with the copies owned, the "
			+ "copies reserved by my open trade offers and the number of my decks that use it. Sorted by name unless "
			+ "sort says otherwise.")
	@Parameter(name = "page", in = ParameterIn.QUERY, description = "Page number, from 0",
		schema = @Schema(type = "integer", defaultValue = "0"))
	@Parameter(name = "size", in = ParameterIn.QUERY, description = "Cards per page, at most 100",
		schema = @Schema(type = "integer", defaultValue = "20"))
	@Parameter(name = "sort", in = ParameterIn.QUERY, description = "name, rarity, type or quantity, optionally "
		+ "followed by ,asc or ,desc (e.g. rarity,desc). Ties are sorted by card name", example = "rarity,desc")
	@ApiResponse(responseCode = "200", description = "Page of my cards returned")
	@ApiResponse(responseCode = "400", description = "Unknown card type, or sort by another property "
		+ "(INVALID_COLLECTION_SORT)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@GetMapping("/collection")
	public ResponseEntity<PageResponse<CollectionEntryResponse>> findMine(
			@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Only cards of this type") @RequestParam(required = false) CardType type,
			@Parameter(description = "Only cards whose name contains this text, ignoring case", example = "wend")
			@RequestParam(required = false) String name,
			@Parameter(description = "Only cards of this rarity (id from GET /rarities)", example = "3")
			@RequestParam(required = false) Integer rarityId,
			@Parameter(description = "Only cards of this faction, as first or second faction (id from GET /factions). "
				+ "Never returns SPELL or HIDDEN cards", example = "2")
			@RequestParam(required = false) Integer factionId,
			@Parameter(hidden = true) Pageable pageable) {
		CollectionFilter filter = new CollectionFilter(type, name, rarityId, factionId);
		return ResponseEntity.ok(collectionService.findMine(currentUserId(jwt), filter, pageable));
	}

	@Operation(summary = "Get my collection progress",
		description = "Active original cards I own over the active original cards in the catalog, overall and per "
			+ "rarity. Owning a variant counts as owning its original; deactivated cards don't count.")
	@ApiResponse(responseCode = "200", description = "Progress returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@GetMapping("/collection/summary")
	public ResponseEntity<CollectionSummaryResponse> summary(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(collectionService.summary(currentUserId(jwt)));
	}

	@Operation(summary = "List another user's collection",
		description = "Returns the cards of another user with the copies available for a trade (owned minus those "
			+ "reserved by their open offers, 0 included), to prepare a trade proposal. Same filters and sort as "
			+ "GET /collection.")
	@Parameter(name = "page", in = ParameterIn.QUERY, description = "Page number, from 0",
		schema = @Schema(type = "integer", defaultValue = "0"))
	@Parameter(name = "size", in = ParameterIn.QUERY, description = "Cards per page, at most 100",
		schema = @Schema(type = "integer", defaultValue = "20"))
	@Parameter(name = "sort", in = ParameterIn.QUERY, description = "name, rarity, type or quantity, optionally "
		+ "followed by ,asc or ,desc. Ties are sorted by card name", example = "name")
	@ApiResponse(responseCode = "200", description = "Page of the user's cards returned")
	@ApiResponse(responseCode = "400", description = "Unknown card type, or sort by another property "
		+ "(INVALID_COLLECTION_SORT)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "The user does not exist or is disabled (USER_NOT_FOUND)")
	@GetMapping("/users/{id}/collection")
	public ResponseEntity<PageResponse<AvailableCardResponse>> findOf(
			@Parameter(description = "Id of the user whose collection to show", example = "12") @PathVariable Integer id,
			@Parameter(description = "Only cards of this type") @RequestParam(required = false) CardType type,
			@Parameter(description = "Only cards whose name contains this text, ignoring case", example = "wend")
			@RequestParam(required = false) String name,
			@Parameter(description = "Only cards of this rarity (id from GET /rarities)", example = "3")
			@RequestParam(required = false) Integer rarityId,
			@Parameter(description = "Only cards of this faction, as first or second faction", example = "2")
			@RequestParam(required = false) Integer factionId,
			@Parameter(hidden = true) Pageable pageable) {
		CollectionFilter filter = new CollectionFilter(type, name, rarityId, factionId);
		return ResponseEntity.ok(collectionService.findOf(id, filter, pageable));
	}

	private static Integer currentUserId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}
}
