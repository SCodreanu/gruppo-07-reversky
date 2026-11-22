package it.downfall.reversky.api.common.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Risposta paginata comune a tutte le liste (docs/api-rest, "Paginazione").
 * <p>
 * Non si restituisce mai direttamente la {@code Page} di Spring Data: il suo JSON non è quello
 * concordato con il frontend e può cambiare tra una versione di Spring e l'altra.
 *
 * @param content       elementi della pagina corrente
 * @param page          numero della pagina, da 0
 * @param size          elementi richiesti per pagina
 * @param totalElements elementi totali su tutte le pagine
 * @param totalPages    numero totale di pagine
 */
public record PageResponse<T>(

	List<T> content,

	@Schema(example = "0")
	int page,

	@Schema(example = "20")
	int size,

	@Schema(example = "134")
	long totalElements,

	@Schema(example = "7")
	int totalPages
) {

	/**
	 * Converte una pagina di Spring Data. Le entità vanno convertite in DTO prima, con
	 * {@code page.map(XxxResponse::from)}, dentro la transazione del service.
	 */
	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
