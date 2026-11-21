package it.downfall.reversky.api.pack.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import it.downfall.reversky.api.inventory.dto.InventoryResponse;
import it.downfall.reversky.api.pack.dto.PackPurchaseRequest;
import it.downfall.reversky.api.pack.dto.PackResponse;
import it.downfall.reversky.api.pack.service.PackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Negozio dei pacchetti (docs/pacchetti). La lista è pubblica; l'acquisto richiede il token.
 */
@Tag(name = "Packs", description = "Packs on sale and their purchase")
@RestController
@RequestMapping("/api/v1/packs")
@RequiredArgsConstructor
public class PackController {

	private final PackService packService;

	@Operation(summary = "List the packs on sale", description = "Public. Returns the active packs with cost, purchase "
		+ "limit, base slots and the bonus odds that can actually happen. With a token, remainingPurchases tells how many "
		+ "packs with a limit the user can still buy.")
	@ApiResponse(responseCode = "200", description = "Packs returned")
	@GetMapping
	public ResponseEntity<List<PackResponse>> findForShop(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
		Integer userId = jwt == null ? null : Integer.valueOf(jwt.getSubject());
		return ResponseEntity.ok(packService.findForShop(userId));
	}

	@Operation(summary = "Buy packs", description = "Spends cost x quantity credits and puts the packs in my inventory. "
		+ "The purchase limit counts the packs already opened too.")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	@ApiResponse(responseCode = "200", description = "Packs bought, updated inventory returned")
	@ApiResponse(responseCode = "400", description = "Quantity missing or not between 1 and 10")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "Pack not found")
	@ApiResponse(responseCode = "422", description = "Pack not on sale, purchase limit reached or insufficient credits")
	@PostMapping("/{id}/purchase")
	public ResponseEntity<InventoryResponse> purchase(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id,
			@Valid @RequestBody PackPurchaseRequest request) {
		return ResponseEntity.ok(packService.purchase(Integer.valueOf(jwt.getSubject()), id, request.quantity()));
	}
}
