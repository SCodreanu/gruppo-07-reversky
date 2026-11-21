package it.downfall.reversky.api.battle.repository;

import java.time.Instant;
import java.util.UUID;

import it.downfall.reversky.api.battle.entity.BattleMode;
import it.downfall.reversky.api.battle.entity.BattleResult;
import it.downfall.reversky.api.battle.entity.BattleStats;

/**
 * Una riga dello storico: solo i campi che servono alla lista, senza log né fotografie dei mazzi (docs/sfide,
 * "Storico").
 */
public record BattleHistoryRow(
	UUID id,
	Instant createdAt,
	BattleMode mode,
	Integer challengerId,
	Integer opponentId,
	BattleResult result,
	BattleStats battleStats,
	Integer challengerRatingDelta,
	Integer opponentRatingDelta
) {
}
