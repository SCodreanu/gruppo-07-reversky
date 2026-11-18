package it.downfall.reversky.api.friendship.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import it.downfall.reversky.api.friendship.dto.FriendRequestResponse;
import it.downfall.reversky.api.friendship.dto.FriendResponse;
import it.downfall.reversky.api.friendship.dto.SendFriendRequest;
import it.downfall.reversky.api.friendship.exception.InvalidFriendshipBoxException;
import it.downfall.reversky.api.friendship.service.FriendshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Amicizie (docs/amicizie). Tutti gli endpoint richiedono l'autenticazione: SecurityConfig
 * non li mette in {@code permitAll} e {@code anyRequest().authenticated()} li copre.
 */
@Tag(name = "Friendships", description = "Friend requests and friend list")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendshipController {

	private final FriendshipService friendshipService;

	@Operation(summary = "List my friends",
		description = "Returns the users I am friend with (ACCEPTED friendships, in both directions).")
	@ApiResponse(responseCode = "200", description = "Friends returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@GetMapping
	public ResponseEntity<List<FriendResponse>> findFriends(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(friendshipService.findFriends(currentUserId(jwt)));
	}

	@Operation(summary = "List my friend requests",
		description = "box=incoming: requests I received (PENDING, where I am the recipient). "
			+ "box=outgoing: requests I sent (PENDING, where I am the requester).")
	@ApiResponse(responseCode = "200", description = "Requests returned")
	@ApiResponse(responseCode = "400", description = "box is neither 'incoming' nor 'outgoing'")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@GetMapping("/requests")
	public ResponseEntity<List<FriendRequestResponse>> findRequests(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "incoming") String box) {
		Integer userId = currentUserId(jwt);
		return switch (box) {
			case "incoming" -> ResponseEntity.ok(friendshipService.findIncomingRequests(userId));
			case "outgoing" -> ResponseEntity.ok(friendshipService.findOutgoingRequests(userId));
			default -> throw new InvalidFriendshipBoxException(box);
		};
	}

	@Operation(summary = "Send a friend request",
		description = "Creates a PENDING row, or accepts an existing PENDING in the opposite direction. "
			+ "Returns 409 if a friendship or friend request already exists.")
	@ApiResponse(responseCode = "200", description = "Friend request sent or auto-accepted")
	@ApiResponse(responseCode = "400", description = "Invalid username, or request to yourself")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "Username not found")
	@ApiResponse(responseCode = "409", description = "Friendship or friend request already exists")
	@PostMapping("/requests")
	public ResponseEntity<FriendRequestResponse> sendRequest(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody SendFriendRequest request) {
		return ResponseEntity.ok(friendshipService.sendRequest(currentUserId(jwt), request.username()));
	}

	@Operation(summary = "Accept a friend request",
		description = "Only the recipient can accept. Turns the PENDING row into ACCEPTED.")
	@ApiResponse(responseCode = "200", description = "Friend request accepted")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "No PENDING request from this user")
	@PostMapping("/requests/{requesterId}/accept")
	public ResponseEntity<FriendRequestResponse> accept(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Integer requesterId) {
		return ResponseEntity.ok(friendshipService.accept(currentUserId(jwt), requesterId));
	}

	@Operation(summary = "Reject a friend request",
		description = "Only the recipient can reject. The PENDING row is deleted, so a new request can be sent later.")
	@ApiResponse(responseCode = "204", description = "Friend request rejected")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "No PENDING request from this user")
	@PostMapping("/requests/{requesterId}/reject")
	public ResponseEntity<Void> reject(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Integer requesterId) {
		friendshipService.reject(currentUserId(jwt), requesterId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Remove a friend or cancel a request",
		description = "Deletes the friendship or pending request with this user, in any direction. "
			+ "Also deletes PENDING battle invites between the two.")
	@ApiResponse(responseCode = "204", description = "Friendship removed")
	@ApiResponse(responseCode = "400", description = "Cannot remove yourself")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "No friendship with this user")
	@DeleteMapping("/{userId}")
	public ResponseEntity<Void> remove(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Integer userId) {
		friendshipService.remove(currentUserId(jwt), userId);
		return ResponseEntity.noContent().build();
	}

	private Integer currentUserId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}
}