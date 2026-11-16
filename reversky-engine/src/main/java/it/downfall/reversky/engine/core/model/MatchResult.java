package it.downfall.reversky.engine.core.model;

import java.util.List;

import it.downfall.reversky.engine.core.log.MatchEvent;

/**
 * Risultato di una partita simulata, restituito all'API (docs/api-engine, risposta {@code 200 OK}).
 *
 * @param result       esito
 * @param endReason    motivo di fine
 * @param totalRounds  round risolti; la preparazione (round 0) non conta (D-42)
 * @param challenger   risultato dello sfidante
 * @param opponent     risultato dell'avversario
 * @param seed         seme ricevuto, per riprodurre la partita
 * @param rulesVersion versione del rule set ricevuta, salvata nello storico
 * @param log          eventi della partita in ordine; copia immutabile
 */
public record MatchResult(
		BattleResult result,
		EndReason endReason,
		int totalRounds,
		PlayerResult challenger,
		PlayerResult opponent,
		long seed,
		String rulesVersion,
		List<MatchEvent> log) {

	/**
	 * Copia il log (CodingRequirements §7). Una lista nulla è un errore di programmazione:
	 * {@code List.copyOf} lancia {@link NullPointerException}.
	 */
	public MatchResult {
		log = List.copyOf(log);
	}
}
