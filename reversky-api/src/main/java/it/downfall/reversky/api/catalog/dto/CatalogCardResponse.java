package it.downfall.reversky.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.entity.CardSubtype;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;

/**
 * Carta restituita da GET /cards, GET /cards/{id} e dagli endpoint di /admin/cards.
 * <p>
 * Rarità e fazioni hanno la forma breve { id, name } di docs/collezione. I tag si aggiungono con #82.
 */
public record CatalogCardResponse(

	@Schema(example = "60")
	Integer id,

	@Schema(example = "Wendigo")
	String name,

	@Schema(example = "CREATURE")
	CardType type,

	@Schema(description = "For a variant, the rarity of its original card")
	RaritySummaryResponse rarity,

	@Schema(description = "Null for SPELL and HIDDEN cards, which belong to every faction. For a variant, the faction of "
		+ "its original card")
	FactionSummaryResponse faction,

	@Schema(description = "Optional second faction (e.g. a Mutant): the card gets the bonuses and penalties of both. "
		+ "Null if the card has only one faction")
	FactionSummaryResponse secondaryFaction,

	@Schema(example = "BURN", description = "Only for SPELL and HIDDEN cards, null otherwise")
	CardSubtype subtype,

	@Schema(example = "WENDIGO", description = "Effect implemented in the engine. Optional for creatures")
	String effectCode,

	@Schema(example = "12", description = "Original card of this event version. Null for an original card")
	Integer variantOfId,

	@Schema(example = "18", description = "Only for creatures, null otherwise")
	Integer atk,

	@Schema(example = "17", description = "Only for creatures, null otherwise")
	Integer def,

	@Schema(example = "1", description = "Null means only the rarity limit applies")
	Integer copyLimit,

	@Schema(example = "/images/cards/wendigo.webp")
	String imagePath,

	@Schema(example = "At the end of each round, the opposing creature loses 1 DEF.")
	String effectDescription,

	@Schema(example = "Hunger and frost that consume everything.")
	String flavorText,

	@Schema(example = "true", description = "An inactive card no longer drops from packs but stays in collections")
	boolean active
) {

	/**
	 * Converte l'entità nel DTO.
	 * <p>
	 * {@code getVariantOf().getId()} non carica l'originale: Hibernate conosce già l'id del proxy.
	 * I nomi di rarità e fazioni invece le caricano, quindi va chiamato dentro la transazione del service.
	 */
	public static CatalogCardResponse from(CatalogCard card) {
		Integer variantOfId = card.isVariant() ? card.getVariantOf().getId() : null;

		return new CatalogCardResponse(
			card.getId(),
			card.getName(),
			card.getCardType(),
			RaritySummaryResponse.from(card.getRarity()),
			FactionSummaryResponse.from(card.getFaction()),
			FactionSummaryResponse.from(card.getSecondaryFaction()),
			card.getCardSubtype(),
			card.getEffectCode(),
			variantOfId,
			card.getAtk(),
			card.getDef(),
			card.getCopyLimit(),
			card.getImagePath(),
			card.getEffectDescription(),
			card.getFlavorText(),
			card.isActive());
	}
}
