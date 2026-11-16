package it.downfall.reversky.engine.core.log;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import it.downfall.reversky.engine.core.model.Side;

/**
 * Evento del log di partita (docs/log-partita, "Formato di un evento").
 *
 * @param sequence numero progressivo dell'evento, da 1
 * @param round    round in cui avviene; 0 per la preparazione
 * @param type     tipo di evento
 * @param side     giocatore a cui si riferisce; nullo se riguarda entrambi
 * @param data     dati strutturati dell'evento; copia immutabile che conserva l'ordine delle chiavi
 * @param message  testo per i giocatori, in inglese
 */
public record MatchEvent(int sequence, int round, EventType type, Side side, Map<String, Object> data,
		String message) {

	/**
	 * Copia {@code data} in una {@link LinkedHashMap}: l'ordine delle chiavi resta quello di inserimento, così il
	 * JSON del log è identico a ogni esecuzione con lo stesso seed (AGENTS.md, regola 2). {@code Map.copyOf} non
	 * va bene perché il suo ordine di iterazione cambia tra un'esecuzione e l'altra della JVM.
	 * Una mappa nulla è un errore di programmazione e lancia {@link NullPointerException}.
	 */
	public MatchEvent {
		data = Collections.unmodifiableMap(new LinkedHashMap<>(data));
	}
}
