package it.downfall.reversky.api.catalog.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Tutti i tag di una fazione per PUT /admin/factions/{id}/tags (API-13): sostituiscono quelli di prima. Una lista
 * vuota toglie tutti i tag; un id ripetuto conta una volta sola.
 *
 * @param tagIds id di tag esistenti
 */
public record FactionTagsRequest(

	@ArraySchema(schema = @Schema(example = "1"))
	@NotNull(message = "Tag ids are required")
	List<@NotNull(message = "Tag id must not be null") Integer> tagIds
) {
}
