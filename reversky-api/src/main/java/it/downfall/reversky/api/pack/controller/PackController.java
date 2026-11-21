package it.downfall.reversky.api.pack.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.pack.dto.PackResponse;
import it.downfall.reversky.api.pack.service.PackService;
import lombok.RequiredArgsConstructor;

/**
 * Negozio dei pacchetti (docs/pacchetti).
 */
@Tag(name = "Packs", description = "Packs on sale")
@RestController
@RequestMapping("/api/v1/packs")
@RequiredArgsConstructor
public class PackController {

	private final PackService packService;

	@Operation(summary = "List the packs on sale", description = "Public. Returns the active packs with cost, purchase "
		+ "limit, base slots and the bonus odds that can actually happen.")
	@ApiResponse(responseCode = "200", description = "Packs returned")
	@GetMapping
	public ResponseEntity<List<PackResponse>> findForShop() {
		return ResponseEntity.ok(packService.findForShop());
	}
}
