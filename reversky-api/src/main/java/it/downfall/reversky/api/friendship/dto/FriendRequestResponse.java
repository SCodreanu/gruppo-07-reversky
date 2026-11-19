package it.downfall.reversky.api.friendship.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.friendship.entity.Friendship;
import it.downfall.reversky.api.user.entity.User;

/**
 * Una richiesta di amicizia (PENDING) o un'amicizia appena accettata.
 * <p>
 * Contiene entrambi i lati, così il client ha sempre tutto senza join client-side.
 */
public record FriendRequestResponse(

	@Schema(example = "1")
	Integer requesterId,

	@Schema(example = "pioggia_inversa")
	String requesterUsername,

	@Schema(example = "2")
	Integer recipientId,

	@Schema(example = "mario_rossi")
	String recipientUsername,

	@Schema(example = "PENDING", allowableValues = {"PENDING", "ACCEPTED"})
	String status,

	@Schema(example = "2026-11-19T16:30:00Z", description = "When the request was sent")
	Instant createdAt
) {

	public static FriendRequestResponse from(Friendship friendship, User requester, User recipient) {
		return new FriendRequestResponse(
			requester.getId(),
			requester.getUsername(),
			recipient.getId(),
			recipient.getUsername(),
			friendship.getStatus().name(),
			friendship.getCreatedAt());
	}
}