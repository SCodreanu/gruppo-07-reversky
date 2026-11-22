package it.downfall.reversky.api.collection.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * L'utente non possiede abbastanza copie di una carta da togliere dalla collezione (docs/collezione).
 * <p>
 * Si controllano le copie possedute, non quelle disponibili: chi chiama (lo scambio) controlla prima la disponibilità,
 * perché le copie che sposta sono riservate proprio da quello scambio (deciso il 20 novembre).
 */
public class NotEnoughCopiesException extends BusinessException {

	public NotEnoughCopiesException(Integer cardId, int owned, int requested) {
		super(Category.RULE_VIOLATION, "NOT_ENOUGH_COPIES", "Not enough copies",
			"The card " + cardId + " has only " + owned + " copies, " + requested + " requested");
	}
}
