package it.downfall.reversky.api.admin.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import it.downfall.reversky.api.common.config.OpenApiConfig;
import it.downfall.reversky.api.common.dto.PageResponse;
import it.downfall.reversky.api.inventory.dto.CreditAdjustmentRequest;
import it.downfall.reversky.api.inventory.dto.InventoryResponse;
import it.downfall.reversky.api.inventory.service.InventoryService;
import it.downfall.reversky.api.user.dto.AdminUserResponse;
import it.downfall.reversky.api.user.dto.RoleChangeRequest;
import it.downfall.reversky.api.user.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestione degli utenti dal backoffice (docs/backoffice, "Utenti").
 * <p>
 * Solo ADMIN: lo garantisce SecurityConfig con la regola su /api/v1/admin/**. L'admin che fa la richiesta si ricava
 * sempre dal token.
 */
@Tag(name = "Admin: users", description = "Backoffice management of the users")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

	private final UserAdminService userAdminService;
	private final InventoryService inventoryService;

	// Pageable nascosto in Swagger come in AdminCardController: l'ordine è fisso (id), si documentano solo page e size
	@Operation(summary = "List the users", description = "Returns the users sorted by id, with role, status and credits.")
	@Parameter(name = "page", in = ParameterIn.QUERY, description = "Page number, from 0", schema = @Schema(type = "integer", defaultValue = "0"))
	@Parameter(name = "size", in = ParameterIn.QUERY, description = "Users per page, at most 100", schema = @Schema(type = "integer", defaultValue = "20"))
	@ApiResponse(responseCode = "200", description = "Page of users returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@GetMapping
	public ResponseEntity<PageResponse<AdminUserResponse>> findAll(
			@Parameter(description = "Only users whose username or email contains this text, ignoring case", example = "pioggia") @RequestParam(required = false) String q,
			@Parameter(description = "Only users with this role (USER or ADMIN). An unknown role returns an empty page", example = "USER") @RequestParam(required = false) String role,
			@Parameter(description = "true = only enabled accounts, false = only disabled accounts, empty = all") @RequestParam(required = false) Boolean enabled,
			@Parameter(hidden = true) Pageable pageable) {
		return ResponseEntity.ok(userAdminService.findAll(q, role, enabled, pageable));
	}

	@Operation(summary = "Disable a user", description = "Disables the account and deletes its refresh tokens. "
		+ "Access tokens already issued stay valid until they expire (at most 15 minutes). Disabling a disabled account changes nothing.")
	@ApiResponse(responseCode = "200", description = "User disabled")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "User not found")
	@ApiResponse(responseCode = "422", description = "An admin cannot disable their own account")
	@PostMapping("/{id}/disable")
	public ResponseEntity<AdminUserResponse> disable(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
		return ResponseEntity.ok(userAdminService.disable(adminId(jwt), id));
	}

	@Operation(summary = "Enable a user", description = "Enables the account again. Enabling an enabled account changes nothing.")
	@ApiResponse(responseCode = "200", description = "User enabled")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "User not found")
	@PostMapping("/{id}/enable")
	public ResponseEntity<AdminUserResponse> enable(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
		return ResponseEntity.ok(userAdminService.enable(adminId(jwt), id));
	}

	@Operation(summary = "Change the role of a user", description = "Sets the role to USER or ADMIN. It applies from the "
		+ "next access token. An admin cannot remove the ADMIN role from their own account.")
	@ApiResponse(responseCode = "200", description = "Role changed")
	@ApiResponse(responseCode = "400", description = "Missing role, or not USER or ADMIN")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "User not found")
	@ApiResponse(responseCode = "422", description = "An admin cannot remove the ADMIN role from their own account")
	@PutMapping("/{id}/role")
	public ResponseEntity<AdminUserResponse> changeRole(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id,
			@Valid @RequestBody RoleChangeRequest request) {
		return ResponseEntity.ok(userAdminService.changeRole(adminId(jwt), id, request.role()));
	}

	@Operation(summary = "Adjust the credits of a user", description = "Adds (positive amount) or removes (negative amount) "
		+ "credits with a mandatory reason. The balance never goes below zero.")
	@ApiResponse(responseCode = "200", description = "Credits adjusted, new balance returned")
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields, or amount equal to zero")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "User not found")
	@ApiResponse(responseCode = "422", description = "The balance would go below zero")
	@PostMapping("/{id}/credits")
	public ResponseEntity<InventoryResponse> adjustCredits(@PathVariable Integer id,
			@Valid @RequestBody CreditAdjustmentRequest request) {
		return ResponseEntity.ok(inventoryService.adjustCredits(id, request.amount(), request.reason()));
	}

	private static Integer adminId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}
}
