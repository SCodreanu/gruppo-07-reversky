package it.downfall.reversky.api.pack.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Un pacchetto del negozio (GET /api/v1/packs, docs/pacchetti): costo, contenuto e probabilità per trasparenza.
 *
 * @param remainingPurchases quanti pacchetti l'utente può ancora comprare; null se il pacchetto non ha limite o se la
 *                           richiesta è senza token
 * @param bonuses            solo i bonus che possono uscire: quelli di una rarità senza carte non compaiono
 */
public record PackResponse(

	@Schema(example = "1")
	Integer id,

	@Schema(example = "Standard")
	String name,

	@Schema(example = "100")
	int cost,

	@Schema(example = "3", nullable = true)
	Integer purchaseLimit,

	@Schema(example = "2", nullable = true)
	Integer remainingPurchases,

	@Schema(example = "packs/base.webp")
	String imagePath,

	@Schema(example = "7")
	int cardCount,

	List<PackSlotResponse> slots,

	List<PackBonusResponse> bonuses
) {
}
