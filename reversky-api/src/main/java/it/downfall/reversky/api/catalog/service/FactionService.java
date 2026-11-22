package it.downfall.reversky.api.catalog.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.catalog.dto.AdminFactionResponse;
import it.downfall.reversky.api.catalog.dto.FactionRequest;
import it.downfall.reversky.api.catalog.dto.FactionResponse;
import it.downfall.reversky.api.catalog.dto.FactionTagsRequest;
import it.downfall.reversky.api.catalog.entity.Faction;
import it.downfall.reversky.api.catalog.entity.Tag;
import it.downfall.reversky.api.catalog.exception.FactionNameAlreadyUsedException;
import it.downfall.reversky.api.catalog.exception.FactionNotFoundException;
import it.downfall.reversky.api.catalog.exception.InvalidFactionTagException;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.TagRepository;
import lombok.RequiredArgsConstructor;

/**
 * Gestione delle fazioni (docs/backoffice, "Dati di riferimento"). Le risposte del backoffice hanno anche i tag
 * (API-13), quelle pubbliche no.
 */
@Service
@RequiredArgsConstructor
public class FactionService {

	private final FactionRepository factionRepository;
	private final TagRepository tagRepository;

	/**
	 * Tutte le fazioni, in ordine di id, senza tag (GET /factions e il motore delle partite).
	 */
	@Transactional(readOnly = true)
	public List<FactionResponse> findAll() {
		return factionRepository.findAllByOrderByIdAsc()
			.stream()
			.map(FactionResponse::from)
			.toList();
	}

	/**
	 * Tutte le fazioni, in ordine di id, con i loro tag (GET /admin/factions).
	 */
	@Transactional(readOnly = true)
	public List<AdminFactionResponse> findAllForAdmin() {
		return factionRepository.findWithTagsByOrderByIdAsc()
			.stream()
			.map(AdminFactionResponse::from)
			.toList();
	}

	/**
	 * Crea una fazione, senza tag: si aggiungono con {@link #replaceTags}.
	 *
	 * @throws FactionNameAlreadyUsedException se il nome è già usato
	 */
	@Transactional
	public AdminFactionResponse create(FactionRequest request) {
		if (factionRepository.existsByName(request.name())) {
			throw new FactionNameAlreadyUsedException(request.name());
		}

		Faction createdFaction = factionRepository.save(new Faction(request.name(), request.emblemPath(), request.description()));
		return AdminFactionResponse.from(createdFaction);
	}

	/**
	 * Modifica nome, emblema e descrizione di una fazione; i tag restano quelli di prima.
	 *
	 * @throws FactionNotFoundException        se la fazione non esiste
	 * @throws FactionNameAlreadyUsedException se il nome è già usato da un'altra fazione
	 */
	@Transactional
	public AdminFactionResponse update(Integer id, FactionRequest request) {
		Faction faction = factionRepository.findById(id)
			.orElseThrow(() -> new FactionNotFoundException());

		if (factionRepository.existsByNameAndIdNot(request.name(), id)) {
			throw new FactionNameAlreadyUsedException(request.name());
		}

		faction.update(request.name(), request.emblemPath(), request.description());
		return AdminFactionResponse.from(faction);
	}

	/**
	 * Sostituisce tutti i tag di una fazione (PUT /admin/factions/{id}/tags, API-13). La fazione si blocca prima di
	 * cambiare i tag, così due sostituzioni contemporanee si mettono in fila e vince l'ultima. Un id ripetuto conta una
	 * volta; se un tag non esiste non cambia niente.
	 *
	 * @throws FactionNotFoundException    se la fazione non esiste
	 * @throws InvalidFactionTagException se uno dei tag non esiste
	 */
	@Transactional
	public AdminFactionResponse replaceTags(Integer id, FactionTagsRequest request) {
		Faction faction = factionRepository.findByIdForUpdate(id)
			.orElseThrow(() -> new FactionNotFoundException());

		Set<Integer> ids = new LinkedHashSet<>(request.tagIds());
		List<Tag> tags = tagRepository.findAllById(ids);
		if (tags.size() != ids.size()) {
			Set<Integer> found = new LinkedHashSet<>(tags.stream().map(Tag::getId).toList());
			throw new InvalidFactionTagException(ids.stream().filter(tagId -> !found.contains(tagId)).toList());
		}

		faction.replaceTags(tags);
		return AdminFactionResponse.from(faction);
	}
}
