package it.downfall.reversky.api.admin.controller;

import java.io.IOException;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
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
 * La creazione e la sostituzione dell'illustrazione (API-14) ricevono il file in multipart/form-data e lo passano al
 * service come byte: il service non conosce i tipi di HTTP.
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
	public ResponseEntity<PageResponse<CatalogCardResponse>> findAll(@Parameter(description = "Only cards of this type") @RequestParam(required = false) CardType type, @Parameter(description = "Only cards whose name contains this text, ignoring case", example = "wend") @RequestParam(required = false) String name, @Parameter(description = "Only cards of this rarity (id from GET /admin/rarities). An unknown id returns an empty page", example = "3") @RequestParam(required = false) Integer rarityId, @Parameter(description = "Only cards of this faction, as first or second faction (id from GET /factions). Never returns SPELL or HIDDEN cards. An unknown id returns an empty page", example = "2") @RequestParam(required = false) Integer factionId, @Parameter(description = "true = only active cards, false = only deactivated cards, empty = all") @RequestParam(required = false) Boolean active, @Parameter(hidden = true) Pageable pageable) {
		return ResponseEntity.ok(catalogCardService.findAll(type, name, rarityId, factionId, active, pageable));
	}

	@Operation(summary = "Create a card with its illustration", description = "multipart/form-data with two parts: 'card', "
		+ "the card as JSON with the same fields and rules as PUT /admin/cards/{id}, and 'image', the illustration "
		+ "(required: PNG, JPEG or WebP, at most 2 MB, recognized from the file content). "
		+ "With variantOfId the card is created as an event variant: the game fields are copied from the original, "
		+ "name, image and flavorText are its own. The new card is active. The image URL is in imageUrl, public.")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
		encoding = @Encoding(name = "card", contentType = MediaType.APPLICATION_JSON_VALUE)))
	@ApiResponse(responseCode = "201", description = "Card created")
	@ApiResponse(responseCode = "400", description = "Missing 'card' or 'image' part, or missing or invalid card fields")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "409", description = "Name already used by another original card")
	@ApiResponse(responseCode = "413", description = "Upload larger than the server's multipart limit")
	@ApiResponse(responseCode = "422", description = "Same card rules as PUT (stats, subtype, effect, factions, rarity, variantOfId, effect code), or the image is empty, over 2 MB or not PNG, JPEG or WebP (CARD_IMAGE_INVALID)")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CatalogCardResponse> create(@Valid @RequestPart("card") CatalogCardUpdateRequest card, @RequestPart("image") MultipartFile image) throws IOException {
		return ResponseEntity.status(HttpStatus.CREATED).body(catalogCardService.create(card, image.getBytes()));
	}

	@Operation(summary = "Replace the illustration of a card", description = "multipart/form-data with one part, 'image' "
		+ "(PNG, JPEG or WebP, at most 2 MB, recognized from the file content). Works for variants and deactivated cards "
		+ "too. The new file gets a new URL; the old one is deleted. If the file is not valid the card keeps its image.")
	@ApiResponse(responseCode = "200", description = "Illustration replaced")
	@ApiResponse(responseCode = "400", description = "Missing 'image' part")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "Card not found")
	@ApiResponse(responseCode = "413", description = "Upload larger than the server's multipart limit")
	@ApiResponse(responseCode = "422", description = "The image is empty, over 2 MB or not PNG, JPEG or WebP (CARD_IMAGE_INVALID)")
	@PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CatalogCardResponse> replaceImage(@PathVariable Integer id, @RequestPart("image") MultipartFile image) throws IOException {
		return ResponseEntity.ok(catalogCardService.replaceImage(id, image.getBytes()));
	}

	@Operation(summary = "Update a card", description ="Replaces the fields of a card. The image is not part of the request. " + "For a variant, the game fields of the request (type, subtype, effect, stats, copy limit) are ignored and copied from the original. " + "Updating an original also updates the game fields of all its variants, rarity included. " + "SPELL, HIDDEN, TERRAIN and ITEM cards must be Rare, and a creature's ATK and DEF must not exceed the maximums of its rarity (no check when the rarity has no maximum). " + "Set variantOfId to make the card a variant of an original card, or leave it empty to make it an original. " + "The effect code must be one of GET /admin/effects for the card type.")
	@ApiResponse(responseCode = "200", description = "Card updated")
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "Card not found")
	@ApiResponse(responseCode = "409", description = "Name already used by another original card")
	@ApiResponse(responseCode = "422", description = "Stats, subtype or effect do not match the card type, ATK or DEF exceed the rarity maximums, the rarity does not exist or is not allowed for the type, variantOfId does not point to an original card, or the effect code is unknown or belongs to another card type (CARD_EFFECT_INVALID)")
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
