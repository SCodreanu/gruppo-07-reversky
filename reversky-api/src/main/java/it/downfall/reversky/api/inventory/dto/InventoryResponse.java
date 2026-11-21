package it.downfall.reversky.api.inventory.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.inventory.entity.Inventory;
import it.downfall.reversky.api.inventory.entity.PackInventory;

/**
 * Inventario dell'utente (GET /api/v1/inventory, docs/crediti): crediti e pacchetti non ancora aperti.
 *
 * @param packs pacchetti da aprire, in ordine di id del pacchetto; lista vuota se non ce ne sono
 */
public record InventoryResponse(

	@Schema(example = "350")
	int credits,

	List<InventoryPackResponse> packs
) {

	/** Le righe dei pacchetti hanno il pacchetto già caricato. */
	public static InventoryResponse from(Inventory inventory, List<PackInventory> packs) {
		return new InventoryResponse(inventory.getCredits(), packs.stream().map(InventoryPackResponse::from).toList());
	}

	/**
	 * Pacchetti non aperti di un tipo.
	 */
	public record InventoryPackResponse(

		@Schema(example = "1")
		Integer packId,

		@Schema(example = "Base Pack")
		String name,

		@Schema(example = "packs/base.webp")
		String imagePath,

		@Schema(example = "2")
		int quantity
	) {

		public static InventoryPackResponse from(PackInventory row) {
			return new InventoryPackResponse(row.getPackId(), row.getPack().getName(), row.getPack().getImagePath(),
				row.getQuantity());
		}
	}
}
