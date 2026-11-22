package it.downfall.reversky.api.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Ingresso nella coda classificata (POST /api/v1/matchmaking/queue, docs/sfide).
 *
 * @param deckId uno dei miei mazzi, valido adesso
 */
public record QueueJoinRequest(

	@Schema(example = "3")
	@NotNull(message = "Deck id is required")
	Integer deckId
) {
}
