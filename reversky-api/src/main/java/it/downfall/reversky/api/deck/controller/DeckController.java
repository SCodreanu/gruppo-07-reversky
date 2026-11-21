package it.downfall.reversky.api.deck.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import it.downfall.reversky.api.deck.dto.DeckRequest;
import it.downfall.reversky.api.deck.dto.DeckResponse;
import it.downfall.reversky.api.deck.service.DeckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Mazzi dell'utente autenticato (docs/mazzi). L'utente si ricava sempre dal token.
 */
@Tag(name = "Decks", description = "Decks of the authenticated user")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/decks")
@RequiredArgsConstructor
public class DeckController {

	private final DeckService deckService;

	@Operation(summary = "List my decks", description = "Returns my decks sorted by name.")
	@ApiResponse(responseCode = "200", description = "Decks returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@GetMapping
	public ResponseEntity<List<DeckResponse>> findMine(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(deckService.findMine(userId(jwt)));
	}

	@Operation(summary = "Get one of my decks")
	@ApiResponse(responseCode = "200", description = "Deck returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "Deck not found, or owned by another user")
	@GetMapping("/{id}")
	public ResponseEntity<DeckResponse> findById(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
		return ResponseEntity.ok(deckService.findMine(userId(jwt), id));
	}

	@Operation(summary = "Create a deck", description = "The name must be unique among my decks.")
	@ApiResponse(responseCode = "201", description = "Deck created")
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "409", description = "I already have a deck with this name")
	@PostMapping
	public ResponseEntity<DeckResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeckRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(deckService.create(userId(jwt), request));
	}

	@Operation(summary = "Replace a deck", description = "Replaces the name of the deck. If the deck is in the ranked queue, the ticket is removed.")
	@ApiResponse(responseCode = "200", description = "Deck updated")
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "Deck not found, or owned by another user")
	@ApiResponse(responseCode = "409", description = "Another of my decks already has this name")
	@PutMapping("/{id}")
	public ResponseEntity<DeckResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id,
			@Valid @RequestBody DeckRequest request) {
		return ResponseEntity.ok(deckService.update(userId(jwt), id, request));
	}

	@Operation(summary = "Delete a deck", description = "Deletes the deck and its ranked queue ticket. "
		+ "A deck used by a pending challenge cannot be deleted.")
	@ApiResponse(responseCode = "204", description = "Deck deleted")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "Deck not found, or owned by another user")
	@ApiResponse(responseCode = "409", description = "The deck is used by a pending challenge")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
		deckService.delete(userId(jwt), id);
		return ResponseEntity.noContent().build();
	}

	private static Integer userId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}
}
