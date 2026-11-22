package it.downfall.reversky.api.catalog.exception;

import java.util.Collection;
import java.util.stream.Collectors;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Un tag della richiesta PUT /admin/cards/{id}/tags non esiste (API-12).
 * <p>
 * È 422 e non 404, come per le fazioni delle carte ({@link InvalidCardFactionException}): la carta dell'URL esiste, è
 * sbagliato un riferimento nel corpo della richiesta.
 */
public class InvalidCardTagException extends BusinessException {

	/**
	 * @param missingIds gli id che non corrispondono a un tag, nell'ordine della richiesta
	 */
	public InvalidCardTagException(Collection<Integer> missingIds) {
		super(Category.RULE_VIOLATION, "CARD_TAG_INVALID", "Invalid card tag",
			"These tags do not exist: " + missingIds.stream().map(String::valueOf).collect(Collectors.joining(", ")));
	}
}
