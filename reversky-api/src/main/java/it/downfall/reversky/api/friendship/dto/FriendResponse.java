package it.downfall.reversky.api.friendship.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.user.entity.User;

/**
 * Un amico nella lista di GET /api/v1/friends (docs/amicizie).
 */
public record FriendResponse(

	@Schema(example = "42")
	Integer userId,

	@Schema(example = "pioggia_inversa")
	String username
) {

	public static FriendResponse from(User user) {
		return new FriendResponse(user.getId(), user.getUsername());
	}
}