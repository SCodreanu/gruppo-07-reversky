package it.downfall.reversky.api.pack.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.pack.entity.Pack;

/**
 * Un pacchetto visto dal backoffice (GET/POST/PUT /api/v1/admin/packs): anche disattivato, con tutti i bonus.
 */
public record AdminPackResponse(

	@Schema(example = "1")
	Integer id,

	@Schema(example = "Base Pack")
	String name,

	@Schema(example = "100")
	int cost,

	@Schema(example = "3", nullable = true)
	Integer purchaseLimit,

	@Schema(example = "packs/base.webp")
	String imagePath,

	@Schema(example = "true")
	boolean active,

	@Schema(example = "7")
	int cardCount,

	List<PackSlotResponse> slots,

	List<PackBonusResponse> bonuses
) {

	/** Slot, bonus e rarità sono LAZY: va chiamato dentro la transazione del service. */
	public static AdminPackResponse from(Pack pack) {
		return new AdminPackResponse(pack.getId(), pack.getName(), pack.getCost(), pack.getPurchaseLimit(),
			pack.getImagePath(), pack.isActive(), pack.getCardCount(),
			pack.getSlots().stream().map(PackSlotResponse::from).toList(),
			pack.getBonuses().stream().map(PackBonusResponse::from).toList());
	}
}
