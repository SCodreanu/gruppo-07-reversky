package it.downfall.reversky.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.entity.CardSubtype;
import it.downfall.reversky.api.catalog.entity.CardType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dati di una carta per PUT /admin/cards/{id} (docs/backoffice, "Carte").
 * <p>
 * Qui si controlla solo la forma dei singoli campi (400). Le regole che legano i campi al tipo
 * (statistiche, sottotipo, effetto, rarità e massimi della rarità) e quelle sulle varianti le
 * controlla il service (422), perché
 * per una variante valgono sui valori copiati dall'originale e non su quelli della richiesta.
 * <p>
 * Non c'è imagePath: l'illustrazione si sostituisce con POST /admin/cards/{id}/image (API-14).
 */
public record CatalogCardUpdateRequest(

	@Schema(example = "Wendigo")
	@NotBlank(message = "Name is required")
	@Size(max = 255, message = "Name must not exceed 255 characters")
	String name,

	@Schema(example = "CREATURE", description = "Ignored for a variant: copied from the original")
	@NotNull(message = "Type is required")
	CardType type,

	@Schema(example = "5", description = "SPELL and HIDDEN must be Common or Uncommon, TERRAIN must be Rare, CREATURE accepts any rarity; "
			+ "a creature's ATK and DEF must not exceed the rarity maximums. Ignored for a variant: copied from the original")
	@NotNull(message = "Rarity is required")
	Integer rarityId,

	@Schema(example = "BURN", description = "Required for SPELL (DAMAGE, SHIELD, BURN) and HIDDEN (SURVIVAL, DAMAGE), empty otherwise. Ignored for a variant")
	CardSubtype subtype,

	@Schema(example = "WENDIGO", description = "Required for SPELL, HIDDEN and TERRAIN, optional for CREATURE. Ignored for a variant")
	@Size(max = 100, message = "Effect code must not exceed 100 characters")
	@Pattern(regexp = ".*\\S.*", message = "Effect code must not be blank")
	String effectCode,

	@Schema(example = "12", description = "Original card of this event version; empty for an original card. It must be an original card, never a variant")
	Integer variantOfId,

	@Schema(example = "18", description = "Required for CREATURE, empty otherwise. Ignored for a variant")
	@Min(value = 0, message = "ATK must not be negative")
	Integer atk,

	@Schema(example = "17", description = "Required for CREATURE, empty otherwise. Ignored for a variant")
	@Min(value = 1, message = "DEF must be at least 1")
	Integer def,

	@Schema(example = "1", description = "Empty means only the rarity limit applies. Ignored for a variant")
	@Min(value = 1, message = "Copy limit must be at least 1")
	Integer copyLimit,

	@Schema(example = "At the end of each round, the opposing creature loses 1 DEF.", description = "Ignored for a variant")
	String effectDescription,

	@Schema(example = "Hunger and frost that consume everything.")
	String flavorText
) {
}
