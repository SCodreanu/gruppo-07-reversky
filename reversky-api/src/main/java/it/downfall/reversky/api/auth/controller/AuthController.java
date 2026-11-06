package it.downfall.reversky.api.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.auth.dto.ForgotPasswordRequest;
import it.downfall.reversky.api.auth.dto.LoginRequest;
import it.downfall.reversky.api.auth.dto.RefreshTokenRequest;
import it.downfall.reversky.api.auth.dto.RegisterRequest;
import it.downfall.reversky.api.auth.dto.ResetPasswordRequest;
import it.downfall.reversky.api.auth.dto.TokenResponse;
import it.downfall.reversky.api.auth.dto.VerifyEmailRequest;
import it.downfall.reversky.api.auth.service.AuthService;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Endpoint di registrazione, login, token e recupero dell'account (docs/autenticazione).
 */
@Tag(name = "Auth", description = "Registration, login, tokens and account recovery")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@Operation(summary = "Register", description = "Creates a USER account and sends the email verification link.")
	@ApiResponse(responseCode = "201", description = "Account created")
	@ApiResponse(responseCode = "400", description = "Invalid username, email or password")
	@ApiResponse(responseCode = "409", description = "Username or email already used")
	@PostMapping("/register")
	public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
		authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@Operation(summary = "Log in", description = "Returns an access token and a refresh token.")
	@ApiResponse(responseCode = "200", description = "Logged in")
	@ApiResponse(responseCode = "400", description = "Missing email or password")
	@ApiResponse(responseCode = "401", description = "Incorrect email or password")
	@ApiResponse(responseCode = "403", description = "Account disabled")
	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@Operation(summary = "Refresh the session", description = "Rotates the refresh token: the one sent stops working and a new pair is returned.")
	@ApiResponse(responseCode = "200", description = "Session refreshed")
	@ApiResponse(responseCode = "400", description = "Missing refresh token")
	@ApiResponse(responseCode = "401", description = "Refresh token invalid, expired or already used")
	@ApiResponse(responseCode = "403", description = "Account disabled")
	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ResponseEntity.ok(authService.refresh(request));
	}

	@Operation(summary = "Log out", description = "Deletes the refresh token. An unknown token still returns 204.")
	@ApiResponse(responseCode = "204", description = "Logged out")
	@ApiResponse(responseCode = "400", description = "Missing refresh token")
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Request a password reset", description = "Sends the reset link if the email is registered. The response is the same either way.")
	@ApiResponse(responseCode = "204", description = "Request accepted")
	@ApiResponse(responseCode = "400", description = "Invalid email")
	@PostMapping("/password/forgot")
	public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		authService.forgotPassword(request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Reset the password", description = "Sets the new password and closes all sessions of the user.")
	@ApiResponse(responseCode = "204", description = "Password changed")
	@ApiResponse(responseCode = "400", description = "Invalid password, or reset token invalid or expired")
	@PostMapping("/password/reset")
	public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		authService.resetPassword(request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Verify the email", description = "Confirms the email with the token received by email.")
	@ApiResponse(responseCode = "204", description = "Email verified")
	@ApiResponse(responseCode = "400", description = "Verification token missing, invalid or expired")
	@PostMapping("/email/verify")
	public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		authService.verifyEmail(request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Send a new verification link", description = "Sends the link to the email of the authenticated user.")
	@ApiResponse(responseCode = "204", description = "Link sent")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "User not found")
	@ApiResponse(responseCode = "409", description = "Email already verified")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	@PostMapping("/email/resend")
	public ResponseEntity<Void> resendEmailVerification(@AuthenticationPrincipal Jwt jwt) {
		authService.resendEmailVerification(Integer.valueOf(jwt.getSubject()));
		return ResponseEntity.noContent().build();
	}

}
