package it.downfall.reversky.api.catalog.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.catalog.dto.FactionRequest;
import it.downfall.reversky.api.catalog.dto.FactionResponse;
import it.downfall.reversky.api.catalog.entity.Faction;
import it.downfall.reversky.api.catalog.exception.FactionNameAlreadyUsedException;
import it.downfall.reversky.api.catalog.exception.FactionNotFoundException;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import lombok.RequiredArgsConstructor;

/**
 * Gestione delle fazioni (docs/backoffice, "Dati di riferimento").
 */
@Service
@RequiredArgsConstructor
public class FactionService {

	private final FactionRepository factionRepository;

	/**
	 * Tutte le fazioni, in ordine di id (GET /factions e GET /admin/factions).
	 */
	@Transactional(readOnly = true)
	public List<FactionResponse> findAll() {
		return factionRepository.findAllByOrderByIdAsc()
			.stream()
			.map(FactionResponse::from)
			.toList();
	}

	/**
	 * Crea una fazione.
	 *
	 * @throws FactionNameAlreadyUsedException se il nome è già usato
	 */
	@Transactional
	public FactionResponse create(FactionRequest request) {
		if (factionRepository.existsByName(request.name())) {
			throw new FactionNameAlreadyUsedException(request.name());
		}

		Faction createdFaction = factionRepository.save(new Faction(request.name(), request.emblemPath(), request.description()));
		return FactionResponse.from(createdFaction);
	}

	/**
	 * Modifica nome, emblema e descrizione di una fazione.
	 *
	 * @throws FactionNotFoundException        se la fazione non esiste
	 * @throws FactionNameAlreadyUsedException se il nome è già usato da un'altra fazione
	 */
	@Transactional
	public FactionResponse update(Integer id, FactionRequest request) {
		Faction faction = factionRepository.findById(id)
			.orElseThrow(() -> new FactionNotFoundException());

		if (factionRepository.existsByNameAndIdNot(request.name(), id)) {
			throw new FactionNameAlreadyUsedException(request.name());
		}

		faction.update(request.name(), request.emblemPath(), request.description());
		return FactionResponse.from(faction);
	}
}
