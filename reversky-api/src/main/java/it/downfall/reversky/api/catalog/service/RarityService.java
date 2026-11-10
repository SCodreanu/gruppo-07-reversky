package it.downfall.reversky.api.catalog.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.catalog.dto.RarityCreateRequest;
import it.downfall.reversky.api.catalog.dto.RarityResponse;
import it.downfall.reversky.api.catalog.dto.RarityUpdateRequest;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.catalog.exception.RarityNameAlreadyUsedException;
import it.downfall.reversky.api.catalog.exception.RarityNotFoundException;
import it.downfall.reversky.api.catalog.exception.RaritySortingOrderAlreadyUsedException;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import lombok.RequiredArgsConstructor;

/**
 * Gestione delle rarità (docs/backoffice, "Dati di riferimento").
 */
@Service
@RequiredArgsConstructor
public class RarityService {

	private final RarityRepository rarityRepository;

	/**
	 * Tutte le rarità, dalla più comune alla più rara (GET /rarities e GET /admin/rarities).
	 */
	@Transactional(readOnly = true)
	public List<RarityResponse> findAll() {
		return  rarityRepository.findAllByOrderBySortingOrderAsc()
			.stream()
			.map(RarityResponse::from)
			.toList();
	}

	/**
	 * Crea una rarità.
	 *
	 * @throws RarityNameAlreadyUsedException         se il nome è già usato
	 * @throws RaritySortingOrderAlreadyUsedException se l'ordine è già usato
	 */
	@Transactional
	public RarityResponse create(RarityCreateRequest request) {
		if (rarityRepository.existsByName(request.name())) {
			throw new RarityNameAlreadyUsedException(request.name());
		}

		if (rarityRepository.existsBySortingOrder(request.sortingOrder())) {
			throw new RaritySortingOrderAlreadyUsedException(request.sortingOrder());
		}

		Rarity createdRarity = rarityRepository.save(new Rarity(request.name(), request.deckLimit(), request.maxPerDeck(), request.sortingOrder()));
		return RarityResponse.from(createdRarity);
	}

	/**
	 * Modifica nome e limiti di una rarità. sortingOrder non cambia mai.
	 *
	 * @throws RarityNotFoundException        se la rarità non esiste
	 * @throws RarityNameAlreadyUsedException se il nome è già usato da un'altra rarità
	 */
	@Transactional
	public RarityResponse update(Integer id, RarityUpdateRequest request) {

		Rarity selectedRarity = rarityRepository.findById(id)
			.orElseThrow(() -> new RarityNotFoundException());

		if (rarityRepository.existsByNameAndIdNot(request.name(), id)) {
			throw new RarityNameAlreadyUsedException(request.name());
		}

		selectedRarity.update(request.name(), request.deckLimit(), request.maxPerDeck());
		return RarityResponse.from(selectedRarity);
	}
}
