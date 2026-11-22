package it.downfall.reversky.api.pack.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.catalog.entity.Tag;
import it.downfall.reversky.api.catalog.service.CatalogCardService;
import it.downfall.reversky.api.catalog.service.RarityService;
import it.downfall.reversky.api.catalog.service.TagService;
import it.downfall.reversky.api.pack.dto.AdminPackResponse;
import it.downfall.reversky.api.pack.dto.PackRequest;
import it.downfall.reversky.api.pack.dto.PackRequest.PackBonusRequest;
import it.downfall.reversky.api.pack.dto.PackRequest.PackSlotRequest;
import it.downfall.reversky.api.pack.entity.Pack;
import it.downfall.reversky.api.pack.entity.PackRarityRule;
import it.downfall.reversky.api.pack.exception.InvalidPackRarityException;
import it.downfall.reversky.api.pack.exception.InvalidPackTagException;
import it.downfall.reversky.api.pack.exception.PackNameAlreadyUsedException;
import it.downfall.reversky.api.pack.exception.PackNotFillableException;
import it.downfall.reversky.api.pack.exception.PackNotFoundException;
import it.downfall.reversky.api.pack.exception.PackRulesViolatedException;
import it.downfall.reversky.api.pack.repository.PackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Pacchetti dal backoffice (docs/backoffice, "Pacchetti"): tipi di pacchetto con slot base e bonus.
 * <p>
 * Un pacchetto nasce disattivato. Si attiva solo se ogni slot base trova almeno una carta con i suoi tag, anche con il
 * ripiego; un pacchetto attivo resta riempibile anche dopo una modifica, tag compresi (API-21). Senza audit finché non
 * arriva API-52.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackAdminService {

	private final PackRepository packRepository;
	private final RarityService rarityService;
	private final CatalogCardService catalogCardService;
	private final TagService tagService;

	/**
	 * Tutti i pacchetti, anche disattivati, in ordine di id (GET /api/v1/admin/packs).
	 */
	@Transactional(readOnly = true)
	public List<AdminPackResponse> findAll() {
		return packRepository.findAllByOrderByIdAsc().stream().map(AdminPackResponse::from).toList();
	}

	/**
	 * Crea un pacchetto disattivato (POST /api/v1/admin/packs).
	 *
	 * @throws PackNameAlreadyUsedException se il nome è già usato
	 * @throws InvalidPackRarityException   se uno slot o un bonus indica una rarità che non esiste
	 * @throws PackRulesViolatedException   se un bonus non è più raro di tutti gli slot base
	 * @throws InvalidPackTagException      se uno dei tag non esiste
	 */
	@Transactional
	public AdminPackResponse create(PackRequest request) {
		String name = request.name().trim();
		requireFreeName(name, null);

		Pack pack = new Pack(name, request.cost(), request.purchaseLimit(), request.imagePath().trim());
		applyRules(pack, request);
		pack = saveChecked(pack, name);

		log.info("Pack {} created", pack.getId());
		return AdminPackResponse.from(pack);
	}

	/**
	 * Sostituisce tutti i campi del pacchetto, slot e bonus compresi (PUT /api/v1/admin/packs/{id}).
	 *
	 * @throws PackNotFoundException        se il pacchetto non esiste
	 * @throws PackNameAlreadyUsedException se il nome è di un altro pacchetto
	 * @throws InvalidPackRarityException   se uno slot o un bonus indica una rarità che non esiste
	 * @throws PackRulesViolatedException   se un bonus non è più raro di tutti gli slot base
	 * @throws InvalidPackTagException      se uno dei tag non esiste
	 * @throws PackNotFillableException     se il pacchetto è attivo e uno slot non troverebbe nessuna carta con i suoi tag
	 */
	@Transactional
	public AdminPackResponse update(Integer id, PackRequest request) {
		Pack pack = getPack(id);
		String name = request.name().trim();
		requireFreeName(name, id);

		pack.update(name, request.cost(), request.purchaseLimit(), request.imagePath().trim());
		applyRules(pack, request);
		if (pack.isActive()) {
			requireFillable(pack);
		}
		return AdminPackResponse.from(saveChecked(pack, name));
	}

	/**
	 * Mette il pacchetto nel negozio (POST /api/v1/admin/packs/{id}/activate).
	 *
	 * @throws PackNotFoundException    se il pacchetto non esiste
	 * @throws PackNotFillableException se uno slot non troverebbe nessuna carta, neanche con il ripiego
	 */
	@Transactional
	public AdminPackResponse activate(Integer id) {
		Pack pack = getPack(id);
		requireFillable(pack);
		pack.activate();
		log.info("Pack {} activated", id);
		return AdminPackResponse.from(pack);
	}

	/**
	 * Toglie il pacchetto dal negozio (POST /api/v1/admin/packs/{id}/deactivate). Quelli già comprati restano
	 * apribili.
	 *
	 * @throws PackNotFoundException se il pacchetto non esiste
	 */
	@Transactional
	public AdminPackResponse deactivate(Integer id) {
		Pack pack = getPack(id);
		pack.deactivate();
		log.info("Pack {} deactivated", id);
		return AdminPackResponse.from(pack);
	}

	private Pack getPack(Integer id) {
		return packRepository.findById(id).orElseThrow(() -> new PackNotFoundException(id));
	}

	// Prima i riferimenti (le rarità e i tag esistono), poi le regole tra slot e bonus
	private void applyRules(Pack pack, PackRequest request) {
		Map<Integer, Rarity> rarities = loadRarities(request);
		List<Tag> tags = loadTags(request);

		Map<Rarity, Integer> slots = new LinkedHashMap<>();
		request.slots().forEach(slot -> slots.put(rarities.get(slot.rarityId()), slot.quantity()));
		Map<Rarity, Integer> bonuses = new LinkedHashMap<>();
		request.bonuses().forEach(bonus -> bonuses.put(rarities.get(bonus.rarityId()), bonus.probabilityUnits()));

		requireBonusesAboveSlots(slots, bonuses);
		pack.replaceSlots(slots);
		pack.replaceBonuses(bonuses);
		pack.replaceTags(tags);
	}

	// tagIds mancante = nessun tag; un id ripetuto conta una volta
	private List<Tag> loadTags(PackRequest request) {
		Set<Integer> ids = new LinkedHashSet<>(request.tagIds() == null ? List.of() : request.tagIds());
		List<Tag> tags = tagService.getTagsByIds(ids);
		if (tags.size() != ids.size()) {
			Set<Integer> found = tags.stream().map(Tag::getId).collect(Collectors.toSet());
			throw new InvalidPackTagException(ids.stream().filter(id -> !found.contains(id)).toList());
		}
		return tags;
	}

	private Map<Integer, Rarity> loadRarities(PackRequest request) {
		List<Integer> ids = Stream.concat(request.slots().stream().map(PackSlotRequest::rarityId),
				request.bonuses().stream().map(PackBonusRequest::rarityId))
			.distinct()
			.toList();
		Map<Integer, Rarity> rarities = rarityService.getRaritiesByIds(ids).stream()
			.collect(Collectors.toMap(Rarity::getId, Function.identity()));

		ids.stream()
			.filter(id -> !rarities.containsKey(id))
			.findFirst()
			.ifPresent(id -> {
				throw new InvalidPackRarityException(id);
			});
		return rarities;
	}

	// Un bonus deve essere più raro di tutti gli slot base: sostituisce uno slot con una carta migliore
	private static void requireBonusesAboveSlots(Map<Rarity, Integer> slots, Map<Rarity, Integer> bonuses) {
		int rarestSlot = slots.keySet().stream().mapToInt(Rarity::getSortingOrder).max().orElse(0);
		bonuses.keySet().stream()
			.filter(bonus -> bonus.getSortingOrder() <= rarestSlot)
			.findFirst()
			.ifPresent(bonus -> {
				throw new PackRulesViolatedException("The bonus rarity " + bonus.getName()
					+ " must be rarer than every base slot");
			});
	}

	private void requireFillable(Pack pack) {
		CardPool pool = new CardPool(catalogCardService.getActiveForPacks(), pack.getTagNames());
		pack.getSlots().stream()
			.map(PackRarityRule::getRarity)
			.filter(rarity -> pool.candidatesFor(rarity).isEmpty())
			.findFirst()
			.ifPresent(rarity -> {
				throw new PackNotFillableException(rarity.getName());
			});
	}

	private void requireFreeName(String name, Integer excludedId) {
		if (packRepository.isNameTaken(name, excludedId)) {
			throw new PackNameAlreadyUsedException(name);
		}
	}

	// Due richieste contemporanee con lo stesso nome passano entrambe il controllo: la seconda trova uq_packs_name
	private Pack saveChecked(Pack pack, String name) {
		try {
			return packRepository.saveAndFlush(pack);
		} catch (DataIntegrityViolationException e) {
			throw new PackNameAlreadyUsedException(name);
		}
	}
}
