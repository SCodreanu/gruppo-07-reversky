package it.downfall.reversky.api.catalog.exception;

import java.util.Collection;
import java.util.stream.Collectors;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Un tag della richiesta PUT /admin/factions/{id}/tags non esiste (API-13).
 * <p>
 * È 422 e non 404, come per le fazioni delle carte ({@link InvalidCardFactionException}): la fazione dell'URL esiste,
 * è sbagliato un riferimento nel corpo della richiesta.
 */
public class InvalidFactionTagException extends BusinessException {

	/**
	 * @param missingIds gli id che non corrispondono a un tag, nell'ordine della richiesta
	 */
	public InvalidFactionTagException(Collection<Integer> missingIds) {
		super(Category.RULE_VIOLATION, "FACTION_TAG_INVALID", "Invalid faction tag",
			"These tags do not exist: " + missingIds.stream().map(String::valueOf).collect(Collectors.joining(", ")));
	}
}
