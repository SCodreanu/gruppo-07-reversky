package it.downfall.reversky.api.pack.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * All'apertura uno slot non trova nessuna carta, perché le carte sono state disattivate dopo l'attivazione del pacchetto (docs/pacchetti): la transazione si annulla e il pacchetto resta nell'inventario.
 */
public class PackSlotEmptyException extends BusinessException {

	public PackSlotEmptyException(Integer packId, String rarityName) {
		super(Category.RULE_VIOLATION, "PACK_SLOT_EMPTY", "Pack cannot be opened",
			"Pack " + packId + " cannot be opened now: no card for the " + rarityName + " slot. The pack stays in your inventory");
	}
}
