package it.downfall.reversky.api.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.inventory.entity.Inventory;

/**
 * Inventario dell'utente (GET /api/v1/inventory, docs/crediti). I pacchetti non aperti arrivano con API-22.
 */
public record InventoryResponse(

	@Schema(example = "350")
	int credits
) {

	public static InventoryResponse from(Inventory inventory) {
		return new InventoryResponse(inventory.getCredits());
	}
}
