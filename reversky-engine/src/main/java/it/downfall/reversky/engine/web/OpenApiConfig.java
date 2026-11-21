package it.downfall.reversky.engine.web;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

/**
 * Configurazione di Swagger / OpenAPI (D-41, docs/api-engine). Swagger UI è su {@code /swagger-ui.html}, come
 * nell'API. Nessuno schema di sicurezza: l'engine non ha autenticazione in rete privata (D-40).
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Reversky Engine", version = "v1",
		description = "Game engine of Reversky: simulates a full match from two decks, the match rules, the faction "
				+ "bonuses and a seed. Called only by reversky-api."))
public class OpenApiConfig {
}
