package it.downfall.reversky.engine.web.dto;

import java.util.Map;

import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.model.Side;

/**
 * Un evento del log di partita (docs/log-partita).
 *
 * @param sequence numero progressivo, da 1
 * @param round    round dell'evento; 0 è la preparazione
 * @param type     tipo di evento
 * @param side     giocatore a cui si riferisce, {@code null} se riguarda la partita
 * @param data     dati dell'evento, con le chiavi nell'ordine in cui sono state scritte
 * @param message  testo per il giocatore, in inglese
 */
public record MatchEventResponse(
		int sequence,
		int round,
		EventType type,
		Side side,
		Map<String, Object> data,
		String message) {
}
