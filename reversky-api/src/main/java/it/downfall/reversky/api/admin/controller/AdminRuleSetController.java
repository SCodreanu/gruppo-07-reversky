package it.downfall.reversky.api.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import it.downfall.reversky.api.ruleset.dto.RuleSetCreateRequest;
import it.downfall.reversky.api.ruleset.dto.RuleSetResponse;
import it.downfall.reversky.api.ruleset.dto.RuleSetSummaryResponse;
import it.downfall.reversky.api.ruleset.dto.RuleSetUpdateRequest;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Regole di gioco dal backoffice (docs/backoffice, "Regole di gioco — rule_sets").
 * <p>
 * Solo ADMIN: lo garantisce SecurityConfig con la regola su /api/v1/admin/**.
 * <p>
 * TODO: popolare {@code createdById} con l'utente autenticato appena il claim è
 * disponibile (la colonna è nullable, quindi per ora il valore resta null).
 */
@Tag(name = "Admin: rule sets", description = "Backoffice management of the game rule sets")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin/rule-sets")
@RequiredArgsConstructor
public class AdminRuleSetController {

	private final RuleSetService ruleSetService;

	@Operation(summary = "List the rule set versions",
		description = "Returns every version, most recent first, with isActive on the current one.")
	@ApiResponse(responseCode = "200", description = "Rule set versions returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@GetMapping
	public ResponseEntity<List<RuleSetSummaryResponse>> findAll() {
		return ResponseEntity.ok(ruleSetService.findAll());
	}

	@Operation(summary = "Create a new rule set version",
		description = "Version must be unique; creatures + spells + hidden + items + terrains must equal deckSize. "
			+ "The new rule set is created inactive.")
	@ApiResponse(responseCode = "201", description = "Rule set created")
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "409", description = "Version already used")
	@ApiResponse(responseCode = "422", description = "Deck parts do not match deckSize")
	@PostMapping
	public ResponseEntity<RuleSetResponse> create(@Valid @RequestBody RuleSetCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ruleSetService.create(request, null));
	}

	@Operation(summary = "Update a rule set version",
		description = "Only a rule set never used by a battle can be modified; otherwise create a new version.")
	@ApiResponse(responseCode = "200", description = "Rule set updated")
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "Rule set not found")
	@ApiResponse(responseCode = "409", description = "Rule set already used by a battle, or version already used")
	@ApiResponse(responseCode = "422", description = "Deck parts do not match deckSize")
	@PutMapping("/{id}")
	public ResponseEntity<RuleSetResponse> update(@PathVariable Integer id, @Valid @RequestBody RuleSetUpdateRequest request) {
		return ResponseEntity.ok(ruleSetService.update(id, request));
	}

	@Operation(summary = "Activate a rule set",
		description = "Deactivates the previous active rule set and activates this one in a single transaction.")
	@ApiResponse(responseCode = "200", description = "Rule set activated")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "Rule set not found")
	@PostMapping("/{id}/activate")
	public ResponseEntity<RuleSetResponse> activate(@PathVariable Integer id) {
		return ResponseEntity.ok(ruleSetService.activate(id));
	}
}