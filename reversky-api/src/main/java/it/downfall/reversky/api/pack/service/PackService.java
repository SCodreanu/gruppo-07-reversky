package it.downfall.reversky.api.pack.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.catalog.service.CatalogCardService;
import it.downfall.reversky.api.pack.dto.PackBonusResponse;
import it.downfall.reversky.api.pack.dto.PackResponse;
import it.downfall.reversky.api.pack.dto.PackSlotResponse;
import it.downfall.reversky.api.pack.entity.Pack;
import it.downfall.reversky.api.pack.repository.PackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Negozio dei pacchetti (docs/pacchetti): elenco dei pacchetti in vendita.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackService {

	private final PackRepository packRepository;
	private final CatalogCardService catalogCardService;

	/**
	 * I pacchetti in vendita, in ordine di id (GET /api/v1/packs, pubblico). I bonus di una rarità senza carte non
	 * compaiono, perché non possono uscire.
	 */
	@Transactional(readOnly = true)
	public List<PackResponse> findForShop() {
		List<Pack> packs = packRepository.findAllByActiveTrueOrderByIdAsc();
		if (packs.isEmpty()) {
			return List.of();
		}

		CardPool pool = new CardPool(catalogCardService.getActiveOriginals());
		return packs.stream().map(pack -> toShopResponse(pack, pool)).toList();
	}

	private static PackResponse toShopResponse(Pack pack, CardPool pool) {
		List<PackBonusResponse> bonuses = pack.getBonuses().stream()
			.filter(bonus -> pool.hasCards(bonus.getRarity()))
			.map(PackBonusResponse::from)
			.toList();
		return new PackResponse(pack.getId(), pack.getName(), pack.getCost(), pack.getPurchaseLimit(), null,
			pack.getImagePath(), pack.getCardCount(), pack.getSlots().stream().map(PackSlotResponse::from).toList(), bonuses);
	}
}
