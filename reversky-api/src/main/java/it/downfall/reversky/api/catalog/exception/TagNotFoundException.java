package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Tag inesistente (PUT /admin/tags/{id}).
 */
public class TagNotFoundException extends BusinessException {

	public TagNotFoundException() {
		super(Category.NOT_FOUND, "TAG_NOT_FOUND", "Tag not found", "This tag does not exist");
	}
}
