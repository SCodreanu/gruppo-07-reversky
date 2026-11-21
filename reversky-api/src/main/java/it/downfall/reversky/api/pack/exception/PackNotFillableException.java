package it.downfall.reversky.api.pack.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Un pacchetto attivo deve poter riempire tutti i suoi slot base, anche con il ripiego (docs/backoffice, "Pacchetti"): la creazione, la modifica o l'attivazione non avviene.
 */
public class PackNotFillableException extends BusinessException {

	public PackNotFillableException(String rarityName) {
		super(Category.RULE_VIOLATION, "PACK_NOT_FILLABLE", "Pack cannot be filled",
			"No card can fill the " + rarityName + " slot, not even with the fallback");
	}
}
