package it.downfall.reversky.api.catalog.service;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.catalog.dto.CatalogCardResponse;
import it.downfall.reversky.api.catalog.dto.CatalogCardUpdateRequest;
import it.downfall.reversky.api.catalog.entity.CardSubtype;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.entity.Faction;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.catalog.exception.CatalogCardNameAlreadyUsedException;
import it.downfall.reversky.api.catalog.exception.CatalogCardNotFoundException;
import it.downfall.reversky.api.catalog.exception.CatalogCardRulesViolatedException;
import it.downfall.reversky.api.catalog.exception.InvalidCardEffectException;
import it.downfall.reversky.api.catalog.exception.InvalidCardFactionException;
import it.downfall.reversky.api.catalog.exception.InvalidCardRarityException;
import it.downfall.reversky.api.catalog.exception.InvalidCardVariantException;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.CatalogCardSpecifications;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;

/**
 * Catalogo carte: consultazione pubblica e gestione dal backoffice (docs/backoffice, "Carte").
 * <p>
 * Le carte non si cancellano mai: si disattivano. Modificare un'originale aggiorna anche le sue
 * varianti, nella stessa transazione.
 */
@Service
@RequiredArgsConstructor
public class CatalogCardService {

	// Ordine fisso delle liste: il sort del client si ignora (una proprietà sconosciuta darebbe 500).
	// id come secondo criterio, così due carte con lo stesso nome hanno sempre lo stesso ordine tra le pagine.
	private static final Sort CARD_ORDER = Sort.by("name", "id");

	private final CatalogCardRepository catalogCardRepository;
	private final RarityRepository rarityRepository;
	private final FactionRepository factionRepository;
	private final EffectService effectService;

	/**
	 * Carte attive, con filtri facoltativi (GET /cards, pubblico).
	 *
	 * @param rarityId  null = tutte le rarità; un id inesistente dà una pagina vuota
	 * @param factionId null = tutte le fazioni; prima o seconda fazione; un id inesistente dà una pagina vuota
	 */
	@Transactional(readOnly = true)
	public PageResponse<CatalogCardResponse> findActive(CardType type, String name, Integer rarityId, Integer factionId,
			Pageable pageable) {

		Specification<CatalogCard> specification = Specification.allOf(
			CatalogCardSpecifications.hasType(type),
			CatalogCardSpecifications.nameContains(name),
			CatalogCardSpecifications.hasRarity(rarityId),
			CatalogCardSpecifications.hasFaction(factionId),
			CatalogCardSpecifications.isActive(true)
		);

		Page<CatalogCard> page = catalogCardRepository.findAll(specification, withCardOrder(pageable));
		return PageResponse.from(page.map(CatalogCardResponse::from));
	}

	/**
	 * Tutte le carte, anche disattivate, con filtri facoltativi (GET /admin/cards).
	 *
	 * @param rarityId  null = tutte le rarità; un id inesistente dà una pagina vuota
	 * @param factionId null = tutte le fazioni; prima o seconda fazione; un id inesistente dà una pagina vuota
	 * @param active    null = attive e disattivate
	 */
	@Transactional(readOnly = true)
	public PageResponse<CatalogCardResponse> findAll(CardType type, String name, Integer rarityId, Integer factionId,
			Boolean active, Pageable pageable) {

		Specification<CatalogCard> specification = Specification.allOf(
			CatalogCardSpecifications.hasType(type),
			CatalogCardSpecifications.nameContains(name),
			CatalogCardSpecifications.hasRarity(rarityId),
			CatalogCardSpecifications.hasFaction(factionId),
			CatalogCardSpecifications.isActive(active)
		);

		Page<CatalogCard> page = catalogCardRepository.findAll(specification, withCardOrder(pageable));
		return PageResponse.from(page.map(CatalogCardResponse::from));
	}

	/**
	 * Una carta, anche disattivata: collezioni e storico la usano ancora (GET /cards/{id}).
	 *
	 * @throws CatalogCardNotFoundException se la carta non esiste
	 */
	@Transactional(readOnly = true)
	public CatalogCardResponse findById(Integer id) {
		return CatalogCardResponse.from(loadCard(id));
	}

	/**
	 * Le carte attive e originali, con rarità e fazioni già caricate: tra queste si pescano le carte dei pacchetti
	 * (docs/pacchetti). Le varianti escono solo dai pacchetti con i loro tag, che arrivano con API-21.
	 */
	@Transactional(readOnly = true)
	public List<CatalogCard> getActiveOriginals() {
		return catalogCardRepository.findAllByIsActiveTrueAndVariantOfIsNullOrderByIdAsc();
	}

	/**
	 * Le carte con quegli id, anche disattivate, per gli altri service (mazzi): rarità, fazioni e carta originale sono
	 * già caricate. Gli id che non esistono mancano dalla lista, senza errore: decide chi chiama.
	 */
	@Transactional(readOnly = true)
	public List<CatalogCard> getCardsByIds(Collection<Integer> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		return catalogCardRepository.findAllByIdIn(ids);
	}

	/**
	 * Modifica una carta (PUT /admin/cards/{id}).
	 * <p>
	 * Per una variante i campi di gioco della richiesta, rarità compresa, si ignorano e si copiano
	 * dall'originale. Per un'originale, dopo la modifica, le sue varianti ricevono gli stessi campi di gioco.
	 * Tutti i controlli avvengono prima di modificare la carta.
	 *
	 * Per un'originale prima si controllano i riferimenti (rarità e fazioni esistono), poi le regole del tipo
	 * (statistiche, sottotipo, effetto, fazioni), poi la rarità ammessa e il nome (deciso il 20 novembre, #81).
	 * Per ultimo l'effetto, che deve essere nella tabella {@code effects} per quel tipo di carta (API-36).
	 *
	 * @throws CatalogCardNotFoundException         se la carta non esiste
	 * @throws InvalidCardVariantException          se variantOfId non indica una carta originale valida
	 * @throws InvalidCardRarityException           se la rarità non esiste o non è ammessa per il tipo
	 * @throws InvalidCardFactionException          se la prima o la seconda fazione non esiste
	 * @throws CatalogCardRulesViolatedException    se statistiche, sottotipo, effetto o fazioni non vanno con il tipo
	 * @throws CatalogCardNameAlreadyUsedException  se il nome è di un'altra carta originale
	 * @throws InvalidCardEffectException           se l'effetto non esiste o è di un altro tipo di carta
	 */
	@Transactional
	public CatalogCardResponse update(Integer id, CatalogCardUpdateRequest request) {

		CatalogCard card = loadCard(id);
		CatalogCard original = resolveOriginal(card, request.variantOfId());
		Rarity rarity = null;
		Faction faction = null;
		Faction secondaryFaction = null;

		if (original == null) {
			rarity = loadRarity(request.rarityId());
			faction = loadFaction(request.factionId());
			secondaryFaction = loadFaction(request.secondaryFactionId());

			validateRules(request.type(), request.subtype(), request.effectCode(), request.atk(), request.def());
			validateFactions(request.type(), request.factionId(), request.secondaryFactionId());
			validateRarity(request.type(), rarity);
		}

		Integer excludedId = (original != null) ? original.getId() : card.getId();
		checkNameAvailable(request.name(), excludedId);

		// Ultimo controllo (API-36): l'effetto è tra quelli della tabella effects e del tipo della carta. Una creatura
		// senza effetto non ha niente da controllare; una variante copia l'effetto dall'originale.
		if (original == null && request.effectCode() != null) {
			effectService.validateForType(request.type(), request.effectCode());
		}

		card.changeVariantOf(original);
		if (original != null) {
			card.copyRulesFrom(original);
		} else {
			card.updateRules(rarity, request.type(), request.subtype(), request.effectCode(), request.effectDescription(), request.atk(), request.def(), request.copyLimit());
			// PUT sostituisce tutta la carta: per Spell e Hidden le fazioni sono null, già controllato
			card.changeFactions(faction, secondaryFaction);
		}

		card.updateDetails(request.name(), request.flavorText());
		if (original == null) {
			for (CatalogCard variant : catalogCardRepository.findAllByVariantOf(card)) {
				variant.copyRulesFrom(card);
			}
		}

		return CatalogCardResponse.from(card);
	}

	/**
	 * Rimette la carta nei pacchetti (POST /admin/cards/{id}/activate). Se è già attiva non cambia niente.
	 *
	 * @throws CatalogCardNotFoundException se la carta non esiste
	 */
	@Transactional
	public CatalogCardResponse activate(Integer id) {
		CatalogCard card = loadCard(id);
		card.activate();
		return CatalogCardResponse.from(card);
	}

	/**
	 * Toglie la carta dai pacchetti (POST /admin/cards/{id}/deactivate); resta nelle collezioni.
	 * Se è già disattivata non cambia niente. Le varianti non cambiano.
	 *
	 * @throws CatalogCardNotFoundException se la carta non esiste
	 */
	@Transactional
	public CatalogCardResponse deactivate(Integer id) {
		CatalogCard card = loadCard(id);
		card.deactivate();
		return CatalogCardResponse.from(card);
	}

	// Stessa pagina e dimensione chieste dal client, ma sempre con CARD_ORDER
	private Pageable withCardOrder(Pageable pageable) {
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), CARD_ORDER);
	}

	private CatalogCard loadCard(Integer id) {
		return catalogCardRepository.findById(id)
			.orElseThrow(() -> new CatalogCardNotFoundException());
	}

	// L'originale indicata da variantOfId, già controllata; null se la carta deve essere originale
	private CatalogCard resolveOriginal(CatalogCard card, Integer variantOfId) {

		if (variantOfId == null) {
			return null;
		}

		if (variantOfId.equals(card.getId())) {
			throw InvalidCardVariantException.variantOfItself();
		}

		CatalogCard original = catalogCardRepository.findById(variantOfId)
			.orElseThrow(() -> InvalidCardVariantException.originalNotFound(variantOfId));

		if (original.isVariant()) {
			throw InvalidCardVariantException.originalIsVariant(variantOfId);
		}

		if (catalogCardRepository.existsByVariantOf(card)) {
			throw InvalidCardVariantException.hasVariants();
		}

		return original;
	}

	// Stesse regole dei CHECK di V3, con i metodi di CardType
	private void validateRules(CardType type, CardSubtype subtype, String effectCode, Integer atk, Integer def) {
		if (type.hasStats() && (atk == null || def == null)) {
			throw CatalogCardRulesViolatedException.missingStats();
		}

		if (!type.hasStats() && (atk != null || def != null)) {
			throw CatalogCardRulesViolatedException.unexpectedStats(type);
		}

		if (!type.acceptsSubtype(subtype)) {
			throw CatalogCardRulesViolatedException.invalidSubtype(type, subtype);
		}

		if (type.requiresEffect() && effectCode == null) {
			throw CatalogCardRulesViolatedException.missingEffect(type);
		}
	}

	// La rarità indicata da rarityId; 422 e non 404 se non esiste, come variantOfId
	private Rarity loadRarity(Integer rarityId) {
		return rarityRepository.findById(rarityId)
			.orElseThrow(() -> InvalidCardRarityException.notFound(rarityId));
	}

	// Regola 4 di docs/backoffice: rarità ammessa per il tipo
	private void validateRarity(CardType type, Rarity rarity) {
		if (!type.acceptsRarity(rarity)) {
			throw InvalidCardRarityException.notAllowedForType(type, rarity);
		}
	}

	// La fazione indicata, o null se l'id è null; 422 e non 404 se non esiste, come la rarità
	private Faction loadFaction(Integer factionId) {
		if (factionId == null) {
			return null;
		}

		return factionRepository.findById(factionId)
			.orElseThrow(() -> InvalidCardFactionException.notFound(factionId));
	}

	// chk_catalog_cards_faction_matches_type e chk_catalog_cards_distinct_factions di V3, e
	// chk_catalog_cards_secondary_faction_creature_only di V14. Una seconda fazione
	// senza la prima cade sempre in uno dei primi due casi: o il tipo vuole la fazione, o non ne ammette.
	private void validateFactions(CardType type, Integer factionId, Integer secondaryFactionId) {
		if (type.hasFaction() && factionId == null) {
			throw CatalogCardRulesViolatedException.missingFaction(type);
		}

		if (!type.hasFaction() && (factionId != null || secondaryFactionId != null)) {
			throw CatalogCardRulesViolatedException.unexpectedFaction(type);
		}

		// V14 (API-71): Terreni e Oggetti hanno una fazione sola
		if (secondaryFactionId != null && !type.acceptsSecondaryFaction()) {
			throw CatalogCardRulesViolatedException.unexpectedSecondaryFaction(type);
		}

		if (secondaryFactionId != null && secondaryFactionId.equals(factionId)) {
			throw CatalogCardRulesViolatedException.sameFactions();
		}
	}

	private void checkNameAvailable(String name, Integer excludedId) {
		if (catalogCardRepository.existsByNameAndVariantOfIsNullAndIdNot(name, excludedId)) {
			throw new CatalogCardNameAlreadyUsedException(name);
		}
	}
}
