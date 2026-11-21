package it.downfall.reversky.api.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Correzione del saldo dal backoffice (POST /api/v1/admin/users/{id}/credits, docs/backoffice).
 *
 * @param amount crediti da aggiungere (positivo) o togliere (negativo); il saldo non scende mai sotto zero
 * @param reason motivo della correzione, obbligatorio: andrà in {@code audit_logs} con API-52
 */
public record CreditAdjustmentRequest(

	@Schema(example = "-50", description = "Credits to add (positive) or remove (negative). Must not be zero.")
	@NotNull(message = "Amount is required")
	Integer amount,

	@Schema(example = "Refund for a pack that failed to open")
	@NotBlank(message = "Reason is required")
	@Size(max = 255, message = "Reason must be at most 255 characters")
	String reason
) {

	// Hibernate Validator legge i metodi "is..." come proprietà: l'errore arriva sul campo "amountNonZero"
	@Schema(hidden = true)
	@AssertTrue(message = "Amount must not be zero")
	public boolean isAmountNonZero() {
		return amount == null || amount != 0;
	}
}
