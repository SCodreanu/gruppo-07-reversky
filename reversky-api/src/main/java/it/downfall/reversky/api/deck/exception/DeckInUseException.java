package it.downfall.reversky.api.deck.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Il mazzo è usato da un invito di sfida ancora in attesa e non si può eliminare (docs/mazzi, docs/sfide).
 */
public class DeckInUseException extends BusinessException {

	public DeckInUseException(Integer deckId) {
		super(Category.CONFLICT, "DECK_IN_USE", "Deck in use",
			"Deck " + deckId + " is used by a pending challenge: withdraw the challenge first");
	}
}
