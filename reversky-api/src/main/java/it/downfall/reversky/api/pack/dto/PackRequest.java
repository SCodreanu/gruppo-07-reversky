package it.downfall.reversky.api.pack.dto;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Creazione o modifica di un pacchetto dal backoffice (POST /api/v1/admin/packs, PUT /api/v1/admin/packs/{id},
 * docs/backoffice). La modifica sostituisce tutto, slot e bonus compresi.
 *
 * @param purchaseLimit massimo di pacchetti per utente; vuoto = nessun limite, obbligatorio per un pacchetto gratuito
 * @param slots         slot base, almeno uno: il numero di carte è la somma delle quantità
 * @param bonuses       probabilità delle carte bonus, anche nessuna
 */
public record PackRequest(

	@Schema(example = "Base Pack")
	@NotBlank(message = "Name is required")
	@Size(max = 255, message = "Name must be at most 255 characters")
	String name,

	@Schema(example = "100")
	@NotNull(message = "Cost is required")
	@Min(value = 0, message = "Cost must not be negative")
	Integer cost,

	@Schema(example = "3", nullable = true)
	@Min(value = 1, message = "Purchase limit must be at least 1")
	Integer purchaseLimit,

	@Schema(example = "packs/base.webp")
	@NotBlank(message = "Image path is required")
	String imagePath,

	@NotNull(message = "Slots are required")
	@Size(min = 1, message = "A pack needs at least one slot")
	List<@NotNull(message = "Slot is required") @Valid PackSlotRequest> slots,

	@NotNull(message = "Bonuses are required, even if empty")
	List<@NotNull(message = "Bonus is required") @Valid PackBonusRequest> bonuses
) {

	// Hibernate Validator legge i metodi "is..." come proprietà: gli errori arrivano su questi nomi di campo

	@Schema(hidden = true)
	@AssertTrue(message = "A free pack needs a purchase limit")
	public boolean isFreePackLimited() {
		return cost == null || cost > 0 || purchaseLimit != null;
	}

	@Schema(hidden = true)
	@AssertTrue(message = "Each rarity can have only one slot")
	public boolean isSlotRaritiesUnique() {
		return unique(slots, PackSlotRequest::rarityId);
	}

	@Schema(hidden = true)
	@AssertTrue(message = "Each rarity can have only one bonus")
	public boolean isBonusRaritiesUnique() {
		return unique(bonuses, PackBonusRequest::rarityId);
	}

	private static <T> boolean unique(List<T> rows, Function<T, Integer> rarityId) {
		if (rows == null) {
			return true;
		}
		List<Integer> ids = rows.stream().filter(Objects::nonNull).map(rarityId).filter(Objects::nonNull).toList();
		return ids.size() == ids.stream().distinct().count();
	}

	/**
	 * Uno slot base: quante carte di una rarità.
	 */
	public record PackSlotRequest(

		@Schema(example = "1")
		@NotNull(message = "Rarity id is required")
		Integer rarityId,

		@Schema(example = "3")
		@NotNull(message = "Quantity is required")
		@Min(value = 1, message = "Quantity must be at least 1")
		Integer quantity
	) {
	}

	/**
	 * Un bonus: probabilità di una carta della rarità, in unità su 10.000 (1.000 = 10%).
	 */
	public record PackBonusRequest(

		@Schema(example = "4")
		@NotNull(message = "Rarity id is required")
		Integer rarityId,

		@Schema(example = "1000")
		@NotNull(message = "Probability is required")
		@Min(value = 0, message = "Probability must be between 0 and 10000")
		@Max(value = 10_000, message = "Probability must be between 0 and 10000")
		Integer probabilityUnits
	) {
	}
}
