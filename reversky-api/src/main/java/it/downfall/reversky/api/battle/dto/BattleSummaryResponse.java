package it.downfall.reversky.api.battle.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.battle.entity.BattleMode;

/**
 * Una partita nello storico (GET /api/v1/battles, docs/sfide), dal punto di vista di chi la guarda.
 *
 * @param completed   false se l'engine non l'ha ancora simulata: si completa con il retry
 * @param outcome     null finché la partita non è completata
 * @param ratingDelta variazione del mio rating, null per le amichevoli
 */
public record BattleSummaryResponse(

	@Schema(example = "0b7e2f6a-3c1d-4f8e-9a2b-5d6c7e8f9a0b")
	UUID battleId,

	@Schema(example = "2026-11-21T10:20:00Z")
	Instant createdAt,

	BattleMode mode,

	PlayerSummaryResponse opponent,

	@Schema(example = "true")
	boolean completed,

	@Schema(nullable = true)
	BattleOutcome outcome,

	@Schema(example = "LIFE_POINTS_ZERO", nullable = true)
	String endReason,

	@Schema(example = "9", nullable = true)
	Integer myFinalLifePoints,

	@Schema(example = "0", nullable = true)
	Integer opponentFinalLifePoints,

	@Schema(example = "37", nullable = true)
	Integer rounds,

	@Schema(nullable = true)
	Integer ratingDelta
) {
}
