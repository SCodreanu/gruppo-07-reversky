package it.downfall.reversky.api.collection.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * {@code sort} della collezione su una proprietà non ammessa: 400 con un codice proprio, come
 * {@code InvalidFriendshipBoxException}, invece di un 500 sulla query (deciso il 20 novembre).
 */
public class InvalidCollectionSortException extends BusinessException {

	public InvalidCollectionSortException(String property) {
		super(Category.INVALID_REQUEST, "INVALID_COLLECTION_SORT", "Invalid sort",
			"Cannot sort by '" + property + "': sort by name, rarity, type or quantity");
	}
}
