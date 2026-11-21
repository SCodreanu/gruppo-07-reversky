package it.downfall.reversky.api.battle.client;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Indirizzo e timeout dell'engine (docs/sfide, "La chiamata all'engine"): configurazione tecnica, non regole di gioco.
 *
 * @param baseUrl es. {@code http://localhost:8081}; in application.properties ha il default locale, sostituibile con
 *                la variabile d'ambiente ENGINE_URL
 * @param timeout tempo massimo di connessione e di risposta
 */
@Validated
@ConfigurationProperties(prefix = "reversky.engine")
public record EngineProperties(

	@NotBlank
	String baseUrl,

	@NotNull
	Duration timeout
) {
}
