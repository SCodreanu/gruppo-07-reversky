package it.downfall.reversky.engine.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Regole della partita, dal rule set attivo nel database dell'API (docs/api-engine). L'engine non ha valori
 * predefiniti: mancano, la richiesta è rifiutata.
 *
 * @param rulesVersion       {@code rule_sets.version}, restituita uguale nel risultato
 * @param startingLifePoints punti vita iniziali, almeno 1
 * @param maxRounds          limite di round totali della partita, almeno 1
 */
public record RulesRequest(

		@NotBlank(message = "Rules version is required")
		String rulesVersion,

		@NotNull(message = "Starting life points are required")
		@Min(value = 1, message = "Starting life points must be at least 1")
		Integer startingLifePoints,

		@NotNull(message = "Max rounds is required")
		@Min(value = 1, message = "Max rounds must be at least 1")
		Integer maxRounds) {
}
