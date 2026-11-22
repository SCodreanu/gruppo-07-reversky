package it.downfall.reversky.api.catalog.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Tutti i tag propri di una carta per PUT /admin/cards/{id}/tags (API-12): sostituiscono quelli di prima. Una lista
 * vuota toglie tutti i tag; un id ripetuto conta una volta sola.
 *
 * @param tagIds id di tag esistenti
 */
public record CardTagsRequest(

	@ArraySchema(schema = @Schema(example = "5"))
	@NotNull(message = "Tag ids are required")
	List<@NotNull(message = "Tag id must not be null") Integer> tagIds
) {
}
