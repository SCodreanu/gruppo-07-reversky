package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Illustrazione della carta non accettata (docs/backoffice, "Illustrazioni"): vuota, oltre la dimensione massima, o
 * non PNG, JPEG o WebP. La carta non si crea e {@code image_path} non cambia.
 */
public class InvalidCardImageException extends BusinessException {

	public InvalidCardImageException(String detail) {
		super(Category.RULE_VIOLATION, "CARD_IMAGE_INVALID", "Invalid card image", detail);
	}
}
