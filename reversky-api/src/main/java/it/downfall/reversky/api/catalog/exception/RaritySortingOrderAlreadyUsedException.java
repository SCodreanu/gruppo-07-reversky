package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Creazione di una rarità con un sortingOrder già usato da un'altra rarità.
 * <p>
 * Solo in creazione: in modifica sortingOrder non cambia.
 */
public class RaritySortingOrderAlreadyUsedException extends BusinessException {

	public RaritySortingOrderAlreadyUsedException(Integer sortingOrder) {
		super(Category.CONFLICT, "RARITY_SORTING_ORDER_TAKEN", "Sorting order already in use", "The sorting order " + sortingOrder + " is already used by another rarity");
	}
}
