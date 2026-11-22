package it.downfall.reversky.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.entity.Tag;

/**
 * Tag restituito dagli endpoint di /admin/tags.
 */
public record TagResponse(

	@Schema(example = "3")
	Integer id,

	@Schema(example = "CHRISTMAS_2026")
	String name
) {

	/**
	 * Converte l'entità nel DTO.
	 */
	public static TagResponse from(Tag tag) {
		return new TagResponse(tag.getId(), tag.getName());
	}
}
