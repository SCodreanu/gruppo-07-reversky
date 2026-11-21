package it.downfall.reversky.api.deck.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Il mazzo non esiste o è di un altro utente (docs/mazzi): nei due casi la risposta è la stessa.
 */
public class DeckNotFoundException extends BusinessException {

	public DeckNotFoundException(Integer deckId) {
		super(Category.NOT_FOUND, "DECK_NOT_FOUND", "Deck not found", "Deck " + deckId + " does not exist");
	}
}
