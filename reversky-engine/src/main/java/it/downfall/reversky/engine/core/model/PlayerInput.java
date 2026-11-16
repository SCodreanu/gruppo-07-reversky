package it.downfall.reversky.engine.core.model;

import java.util.List;

/**
 * Giocatore ricevuto nella richiesta: id dell'utente e mazzo nell'ordine dato dall'API, prima del mescolamento.
 *
 * @param userId id dell'utente nel database dell'API, restituito uguale nel risultato
 * @param deck   una voce per ogni copia; copia immutabile della lista ricevuta
 */
public record PlayerInput(int userId, List<CardDefinition> deck) {

	/**
	 * Copia il mazzo, così chi ha costruito l'input non può cambiarlo durante la simulazione
	 * (CodingRequirements §7). Una lista o una carta nulla è un errore di programmazione: {@code List.copyOf}
	 * lancia {@link NullPointerException}.
	 */
	public PlayerInput {
		deck = List.copyOf(deck);
	}
}
