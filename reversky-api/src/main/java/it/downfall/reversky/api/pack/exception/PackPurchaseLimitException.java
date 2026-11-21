package it.downfall.reversky.api.pack.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * L'acquisto supererebbe il {@code purchase_limit} del pacchetto, contando anche quelli già aperti (docs/pacchetti).
 */
public class PackPurchaseLimitException extends BusinessException {

	public PackPurchaseLimitException(int limit, long alreadyBought, int requested) {
		super(Category.RULE_VIOLATION, "PACK_PURCHASE_LIMIT", "Purchase limit reached",
			"You can buy at most " + limit + " of this pack: you already bought " + alreadyBought + ", " + requested + " requested");
	}
}
