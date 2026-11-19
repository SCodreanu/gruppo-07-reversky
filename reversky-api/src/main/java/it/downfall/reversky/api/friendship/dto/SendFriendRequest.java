package it.downfall.reversky.api.friendship.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body di POST /api/v1/friends/requests (docs/amicizie).
 */
public record SendFriendRequest(

	@Schema(example = "pioggia_inversa")
	@NotBlank(message = "Username is required")
	@Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
	@Pattern(regexp = "^[A-Za-z0-9_]{3,30}$", message = "Username can only contain letters, digits and underscores")
	String username
) {
}