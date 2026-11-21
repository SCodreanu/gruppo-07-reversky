package it.downfall.reversky.api.pack.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.catalog.service.CatalogCardService;
import it.downfall.reversky.api.inventory.dto.InventoryResponse;
import it.downfall.reversky.api.inventory.entity.CreditReason;
import it.downfall.reversky.api.inventory.exception.InsufficientCreditsException;
import it.downfall.reversky.api.inventory.service.InventoryService;
import it.downfall.reversky.api.pack.dto.PackBonusResponse;
import it.downfall.reversky.api.pack.dto.PackResponse;
import it.downfall.reversky.api.pack.dto.PackSlotResponse;
import it.downfall.reversky.api.pack.entity.Pack;
import it.downfall.reversky.api.pack.exception.PackNotAvailableException;
import it.downfall.reversky.api.pack.exception.PackNotFoundException;
import it.downfall.reversky.api.pack.exception.PackPurchaseLimitException;
import it.downfall.reversky.api.pack.repository.PackOpeningRepository;
import it.downfall.reversky.api.pack.repository.PackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Negozio dei pacchetti (docs/pacchetti): elenco dei pacchetti in vendita e acquisto.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackService {

	private final PackRepository packRepository;
	private final PackOpeningRepository packOpeningRepository;
	private final CatalogCardService catalogCardService;
	private final InventoryService inventoryService;

	/**
	 * I pacchetti in vendita, in ordine di id (GET /api/v1/packs, pubblico). I bonus di una rarità senza carte non
	 * compaiono, perché non possono uscire.
	 *
	 * @param userId utente del token, oppure null senza token: serve solo a calcolare quanti pacchetti può ancora
	 *               comprare
	 */
	@Transactional(readOnly = true)
	public List<PackResponse> findForShop(Integer userId) {
		List<Pack> packs = packRepository.findAllByActiveTrueOrderByIdAsc();
		if (packs.isEmpty()) {
			return List.of();
		}

		CardPool pool = new CardPool(catalogCardService.getActiveOriginals());
		return packs.stream().map(pack -> toShopResponse(pack, pool, remainingPurchases(userId, pack))).toList();
	}

	/**
	 * Compra pacchetti con i crediti e li mette nell'inventario, in una sola transazione (POST
	 * /api/v1/packs/{id}/purchase). Un pacchetto gratuito scrive comunque la sua riga {@code PACK_PURCHASE} da 0.
	 *
	 * @param quantity pacchetti da comprare, da 1 a 10 (lo controlla la richiesta)
	 * @return l'inventario aggiornato
	 * @throws PackNotFoundException         se il pacchetto non esiste
	 * @throws PackNotAvailableException     se il pacchetto è disattivato
	 * @throws PackPurchaseLimitException    se l'acquisto supera il limite, contando anche i pacchetti già aperti
	 * @throws InsufficientCreditsException  se i crediti non bastano: saldo e pacchetti restano invariati
	 */
	@Transactional
	public InventoryResponse purchase(Integer userId, Integer packId, int quantity) {
		Pack pack = packRepository.findById(packId).orElseThrow(() -> new PackNotFoundException(packId));
		if (!pack.isActive()) {
			throw new PackNotAvailableException(packId);
		}

		// Il blocco viene prima del conteggio: due acquisti contemporanei non superano il limite insieme
		inventoryService.lockInventories(List.of(userId));
		requireWithinLimit(userId, pack, quantity);

		inventoryService.spendCredits(userId, Math.multiplyExact(pack.getCost(), quantity), CreditReason.PACK_PURCHASE);
		inventoryService.addPacks(userId, pack, quantity);

		log.info("User {} bought {} pack(s) {}", userId, quantity, packId);
		return inventoryService.getInventory(userId);
	}

	private void requireWithinLimit(Integer userId, Pack pack, int quantity) {
		if (pack.getPurchaseLimit() == null) {
			return;
		}
		long bought = alreadyBought(userId, pack);
		if (bought + quantity > pack.getPurchaseLimit()) {
			throw new PackPurchaseLimitException(pack.getPurchaseLimit(), bought, quantity);
		}
	}

	// Già comprati = aperti (pack_openings) + ancora da aprire (pack_inventory): aprire non cambia il conteggio
	private long alreadyBought(Integer userId, Pack pack) {
		return packOpeningRepository.countByUserIdAndPackId(userId, pack.getId())
			+ inventoryService.getPackQuantity(userId, pack.getId());
	}

	private Integer remainingPurchases(Integer userId, Pack pack) {
		if (userId == null || pack.getPurchaseLimit() == null) {
			return null;
		}
		return (int) Math.max(0, pack.getPurchaseLimit() - alreadyBought(userId, pack));
	}

	private static PackResponse toShopResponse(Pack pack, CardPool pool, Integer remainingPurchases) {
		List<PackBonusResponse> bonuses = pack.getBonuses().stream()
			.filter(bonus -> pool.hasCards(bonus.getRarity()))
			.map(PackBonusResponse::from)
			.toList();
		return new PackResponse(pack.getId(), pack.getName(), pack.getCost(), pack.getPurchaseLimit(), remainingPurchases,
			pack.getImagePath(), pack.getCardCount(), pack.getSlots().stream().map(PackSlotResponse::from).toList(), bonuses);
	}
}
