package it.downfall.reversky.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.entity.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dati di un tag per POST /admin/tags e PUT /admin/tags/{id} (docs/backoffice). Il nome si controlla così com'è,
 * senza toglierne gli spazi né renderlo maiuscolo (deciso il 22 novembre con API-07): il backoffice lo manda già nel
 * formato giusto.
 *
 * @param name maiuscole, cifre e trattino basso, al massimo 100 caratteri
 */
public record TagRequest(

	@Schema(example = "CHRISTMAS_2026")
	@NotBlank(message = "Name is required")
	@Size(max = Tag.NAME_MAX_LENGTH, message = "Name must not exceed 100 characters")
	@Pattern(regexp = Tag.NAME_PATTERN, message = "Name must contain only uppercase letters, digits and underscores")
	String name
) {
}
