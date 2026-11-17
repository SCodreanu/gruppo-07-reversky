package it.downfall.reversky.engine.web.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Un giocatore con il suo mazzo (docs/api-engine).
 *
 * @param userId id dell'utente nell'API
 * @param deck   una voce per ogni copia, nell'ordine in cui l'API la manda (il mescolamento lo fa l'engine)
 */
public record PlayerRequest(

		@NotNull(message = "User id is required")
		Integer userId,

		@NotEmpty(message = "Deck must not be empty")
		List<@NotNull(message = "Card must not be null") @Valid CardRequest> deck) {
}
