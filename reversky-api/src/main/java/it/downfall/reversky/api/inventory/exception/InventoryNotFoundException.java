package it.downfall.reversky.api.inventory.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * L'utente non ha un inventario: non esiste, perché la registrazione e la migrazione V10 lo creano per tutti.
 */
public class InventoryNotFoundException extends BusinessException {

	public InventoryNotFoundException(Integer userId) {
		super(Category.NOT_FOUND, "INVENTORY_NOT_FOUND", "Inventory not found",
			"User " + userId + " has no inventory");
	}
}
