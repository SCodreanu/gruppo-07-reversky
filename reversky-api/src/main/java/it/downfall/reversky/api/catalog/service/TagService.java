package it.downfall.reversky.api.catalog.service;

import java.util.Collection;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.catalog.dto.TagRequest;
import it.downfall.reversky.api.catalog.dto.TagResponse;
import it.downfall.reversky.api.catalog.entity.Tag;
import it.downfall.reversky.api.catalog.exception.TagNameAlreadyUsedException;
import it.downfall.reversky.api.catalog.exception.TagNotFoundException;
import it.downfall.reversky.api.catalog.repository.TagRepository;
import lombok.RequiredArgsConstructor;

/**
 * Tag del catalogo dal backoffice (docs/backoffice, API-07). I collegamenti a fazioni e carte sono in
 * {@code FactionService} e {@code CatalogCardService} (API-13, API-12); i pacchetti leggono i tag da qui (API-21).
 */
@Service
@RequiredArgsConstructor
public class TagService {

	private final TagRepository tagRepository;

	/**
	 * Tutti i tag, in ordine alfabetico (GET /admin/tags).
	 */
	@Transactional(readOnly = true)
	public List<TagResponse> findAll() {
		return tagRepository.findAllByOrderByNameAsc()
			.stream()
			.map(TagResponse::from)
			.toList();
	}

	/**
	 * I tag con quegli id, per le altre funzionalità (pacchetti). Gli id che non esistono mancano dalla lista, senza
	 * errore: decide chi chiama.
	 */
	@Transactional(readOnly = true)
	public List<Tag> getTagsByIds(Collection<Integer> ids) {
		return tagRepository.findAllById(ids);
	}

	/**
	 * Crea un tag.
	 *
	 * @throws TagNameAlreadyUsedException se il nome è già usato
	 */
	@Transactional
	public TagResponse create(TagRequest request) {
		if (tagRepository.existsByName(request.name())) {
			throw new TagNameAlreadyUsedException(request.name());
		}
		return TagResponse.from(saveChecked(new Tag(request.name())));
	}

	/**
	 * Rinomina un tag; ridargli il suo stesso nome va bene.
	 *
	 * @throws TagNotFoundException        se il tag non esiste
	 * @throws TagNameAlreadyUsedException se il nome è già usato da un altro tag
	 */
	@Transactional
	public TagResponse update(Integer id, TagRequest request) {
		Tag tag = tagRepository.findById(id)
			.orElseThrow(TagNotFoundException::new);
		if (tagRepository.existsByNameAndIdNot(request.name(), id)) {
			throw new TagNameAlreadyUsedException(request.name());
		}

		tag.rename(request.name());
		return TagResponse.from(saveChecked(tag));
	}

	// Due richieste contemporanee con lo stesso nome passano entrambe il controllo: la seconda trova il vincolo
	// uq_tags_name, e anche lei riceve 409 invece di 500 (come i nomi dei mazzi)
	private Tag saveChecked(Tag tag) {
		try {
			return tagRepository.saveAndFlush(tag);
		} catch (DataIntegrityViolationException e) {
			throw new TagNameAlreadyUsedException(tag.getName());
		}
	}
}
