package it.downfall.reversky.api.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Invito a una sfida amichevole (POST /api/v1/battle-invites, docs/sfide).
 *
 * @param opponentId un amico
 * @param deckId     uno dei miei mazzi, valido adesso
 */
public record BattleInviteRequest(

	@Schema(example = "12")
	@NotNull(message = "Opponent id is required")
	Integer opponentId,

	@Schema(example = "3")
	@NotNull(message = "Deck id is required")
	Integer deckId
) {
}
