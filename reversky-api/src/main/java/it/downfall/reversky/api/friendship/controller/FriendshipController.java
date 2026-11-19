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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
		description = "Returns the users I am friend with (ACCEPTED friendships, whoever sent the request). "
			+ "Pending requests are not included: they are in GET /friends/requests.")
	@ApiResponse(responseCode = "200", description = "Friends returned (empty list if none)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@GetMapping
	public ResponseEntity<List<FriendResponse>> findFriends(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(friendshipService.findFriends(currentUserId(jwt)));
	}

	@Operation(summary = "List my friend requests",
		description = "box=incoming: requests I received (PENDING, where I am the recipient). "
			+ "box=outgoing: requests I sent (PENDING, where I am the requester). "
			+ "Both lists are sorted from the newest request.")
	@ApiResponse(responseCode = "200", description = "Requests returned (empty list if none)")
	@ApiResponse(responseCode = "400", description = "box is neither 'incoming' nor 'outgoing' (INVALID_FRIENDSHIP_BOX)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@GetMapping("/requests")
	public ResponseEntity<List<FriendRequestResponse>> findRequests(
			@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Which requests to list", schema = @Schema(allowableValues = {"incoming", "outgoing"}, defaultValue = "incoming"))
			@RequestParam(defaultValue = "incoming") String box) {
		Integer userId = currentUserId(jwt);
		return switch (box) {
			case "incoming" -> ResponseEntity.ok(friendshipService.findIncomingRequests(userId));
			case "outgoing" -> ResponseEntity.ok(friendshipService.findOutgoingRequests(userId));
			default -> throw new InvalidFriendshipBoxException(box);
		};
	}

	@Operation(summary = "Send a friend request",
		description = "Creates a PENDING request to the user with this username. If that user already sent me "
			+ "a PENDING request, it is accepted instead and the response has status ACCEPTED (still a single row). "
			+ "Usernames are case-sensitive.")
	@ApiResponse(responseCode = "200", description = "Friend request sent (PENDING) or opposite request accepted (ACCEPTED)")
	@ApiResponse(responseCode = "400", description = "Missing or malformed username (VALIDATION_FAILED), or request to yourself (FRIENDSHIP_SELF_REQUEST)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "No user with this username (USER_NOT_FOUND)")
	@ApiResponse(responseCode = "409", description = "Already friends, request already sent, or an opposite request sent at the same moment: "
		+ "repeating it accepts that request (FRIENDSHIP_ALREADY_EXISTS)")
	@PostMapping("/requests")
	public ResponseEntity<FriendRequestResponse> sendRequest(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody SendFriendRequest request) {
		return ResponseEntity.ok(friendshipService.sendRequest(currentUserId(jwt), request.username()));
	}

	@Operation(summary = "Accept a friend request",
		description = "Only the recipient can accept. Turns the PENDING request into ACCEPTED.")
	@ApiResponse(responseCode = "200", description = "Friend request accepted")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "No PENDING request from this user to me, including when I am not the recipient "
		+ "or the request was already accepted (FRIENDSHIP_REQUEST_NOT_FOUND)")
	@PostMapping("/requests/{requesterId}/accept")
	public ResponseEntity<FriendRequestResponse> accept(
			@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Id of the user who sent the request", example = "42") @PathVariable Integer requesterId) {
		return ResponseEntity.ok(friendshipService.accept(currentUserId(jwt), requesterId));
	}

	@Operation(summary = "Reject a friend request",
		description = "Only the recipient can reject. The PENDING request is deleted, so either user can send a new one later.")
	@ApiResponse(responseCode = "204", description = "Friend request rejected")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "No PENDING request from this user to me, including when I am not the recipient "
		+ "or the request was already accepted (FRIENDSHIP_REQUEST_NOT_FOUND)")
	@PostMapping("/requests/{requesterId}/reject")
	public ResponseEntity<Void> reject(
			@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Id of the user who sent the request", example = "42") @PathVariable Integer requesterId) {
		friendshipService.reject(currentUserId(jwt), requesterId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Remove a friend or cancel a request",
		description = "Deletes the friendship with this user, or the PENDING request between us in either direction. "
			+ "Also deletes the PENDING battle invites between the two of us, in both directions.")
	@ApiResponse(responseCode = "204", description = "Friendship or request removed")
	@ApiResponse(responseCode = "400", description = "userId is my own id (FRIENDSHIP_SELF_REQUEST)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "404", description = "No friendship or request with this user (FRIENDSHIP_NOT_FOUND)")
	@DeleteMapping("/{userId}")
	public ResponseEntity<Void> remove(
			@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Id of the other user", example = "42") @PathVariable Integer userId) {
		friendshipService.remove(currentUserId(jwt), userId);
		return ResponseEntity.noContent().build();
	}

	private Integer currentUserId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}
}