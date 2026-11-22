package it.downfall.reversky.api.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Accettazione di una sfida (POST /api/v1/battle-invites/{id}/accept, docs/sfide).
 *
 * @param deckId uno dei miei mazzi, valido adesso
 */
public record BattleAcceptRequest(

	@Schema(example = "8")
	@NotNull(message = "Deck id is required")
	Integer deckId
) {
}
