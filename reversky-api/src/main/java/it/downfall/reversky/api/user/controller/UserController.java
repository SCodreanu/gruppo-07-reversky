package it.downfall.reversky.api.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import it.downfall.reversky.api.user.dto.UserResponse;
import it.downfall.reversky.api.user.service.UserService;
import lombok.RequiredArgsConstructor;

/**
 * Endpoint dell'utente autenticato (docs/autenticazione, "Chi sono io").
 */
@Tag(name = "Users", description = "Profile of the authenticated user")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@Operation(summary = "Get my profile", description = "Returns the profile of the user who owns the access token.")
	@ApiResponse(responseCode = "200", description = "Profile returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "User not found")
	@GetMapping("/me")
	public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
		Integer userId = Integer.valueOf(jwt.getSubject());
		return ResponseEntity.ok().body(userService.getProfile(userId));
	}
}
