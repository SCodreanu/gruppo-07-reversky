package it.downfall.reversky.engine.web.dto;

import java.util.List;

import it.downfall.reversky.engine.core.model.BattleResult;
import it.downfall.reversky.engine.core.model.EndReason;

/**
 * Risposta {@code 200} di {@code POST /api/v1/simulations} (docs/api-engine). L'API salva {@code log} in
 * {@code battles.rounds_log} e il resto in {@code battles.battle_stats}.
 *
 * @param result       esito, uguale all'enum {@code BattleResult} del database
 * @param endReason    motivo della fine
 * @param totalRounds  round risolti; la preparazione (round 0) non conta (D-42)
 * @param challenger   risultato dello sfidante
 * @param opponent     risultato dell'avversario
 * @param seed         seed ricevuto
 * @param rulesVersion versione delle regole ricevuta
 * @param log          eventi della partita in ordine (docs/log-partita)
 */
public record SimulationResponse(
		BattleResult result,
		EndReason endReason,
		int totalRounds,
		PlayerResultResponse challenger,
		PlayerResultResponse opponent,
		long seed,
		String rulesVersion,
		List<MatchEventResponse> log) {
}
