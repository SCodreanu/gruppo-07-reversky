package it.downfall.reversky.api.deck.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * L'utente ha già un altro mazzo con questo nome (vincolo {@code uq_decks_user_id_name}, docs/mazzi).
 */
public class DeckNameAlreadyUsedException extends BusinessException {

	public DeckNameAlreadyUsedException(String name) {
		super(Category.CONFLICT, "DECK_NAME_ALREADY_USED", "Deck name already used",
			"You already have a deck named '" + name + "'");
	}
}
