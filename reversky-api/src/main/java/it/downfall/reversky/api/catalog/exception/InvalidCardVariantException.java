package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * {@code variantOfId} non valido (docs/backoffice, "Carte", regola 8): una variante indica sempre
 * una carta originale, e una carta che ha varianti non può diventare a sua volta variante.
 * <p>
 * Anche l'originale inesistente è 422 e non 404: la risorsa dell'URL esiste, è sbagliato un
 * riferimento nel corpo della richiesta.
 */
public class InvalidCardVariantException extends BusinessException {

	private InvalidCardVariantException(String detail) {
		super(Category.RULE_VIOLATION, "CARD_VARIANT_INVALID", "Invalid card variant", detail);
	}

	/**
	 * La carta indicata come originale non esiste.
	 */
	public static InvalidCardVariantException originalNotFound(Integer variantOfId) {
		return new InvalidCardVariantException("The original card " + variantOfId + " does not exist");
	}

	/**
	 * La carta indicata come originale è a sua volta una variante.
	 */
	public static InvalidCardVariantException originalIsVariant(Integer variantOfId) {
		return new InvalidCardVariantException("The card " + variantOfId + " is a variant: a variant must point to an original card");
	}

	/**
	 * La carta indica se stessa come originale.
	 */
	public static InvalidCardVariantException variantOfItself() {
		return new InvalidCardVariantException("A card cannot be a variant of itself");
	}

	/**
	 * La carta ha già delle varianti, quindi deve restare originale.
	 */
	public static InvalidCardVariantException hasVariants() {
		return new InvalidCardVariantException("This card has variants, so it cannot become a variant itself");
	}
}
