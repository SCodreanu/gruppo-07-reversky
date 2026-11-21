package it.downfall.reversky.api.pack.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Acquisto di pacchetti (POST /api/v1/packs/{id}/purchase, docs/pacchetti).
 */
public record PackPurchaseRequest(

	@Schema(example = "1")
	@NotNull(message = "Quantity is required")
	@Min(value = 1, message = "Quantity must be between 1 and 10")
	@Max(value = 10, message = "Quantity must be between 1 and 10")
	Integer quantity
) {
}
