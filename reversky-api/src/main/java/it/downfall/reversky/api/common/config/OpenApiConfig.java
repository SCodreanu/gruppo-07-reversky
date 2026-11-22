package it.downfall.reversky.api.common.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Configurazione di Swagger / OpenAPI (docs/api-rest).
 * <p>
 * Dichiara lo schema di sicurezza Bearer: gli endpoint protetti lo richiamano con
 * {@code @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)} e in Swagger UI compare il pulsante "Authorize".
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Reversky API", version = "v1", description = "REST backend of Reversky"))
@SecurityScheme(
	name = OpenApiConfig.BEARER_AUTH,
	type = SecuritySchemeType.HTTP,
	scheme = "bearer",
	bearerFormat = "JWT")
public class OpenApiConfig {

	public static final String BEARER_AUTH = "bearerAuth";
}
