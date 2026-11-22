package it.downfall.reversky.api.pack.exception;

import java.util.Collection;
import java.util.stream.Collectors;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Un tag di {@code tagIds} in POST o PUT /admin/packs non esiste (API-21).
 * <p>
 * È 422 e non 404, come per le rarità degli slot ({@link InvalidPackRarityException}): è sbagliato un riferimento nel
 * corpo della richiesta.
 */
public class InvalidPackTagException extends BusinessException {

	/**
	 * @param missingIds gli id che non corrispondono a un tag, nell'ordine della richiesta
	 */
	public InvalidPackTagException(Collection<Integer> missingIds) {
		super(Category.RULE_VIOLATION, "PACK_TAG_INVALID", "Invalid pack tag",
			"These tags do not exist: " + missingIds.stream().map(String::valueOf).collect(Collectors.joining(", ")));
	}
}
