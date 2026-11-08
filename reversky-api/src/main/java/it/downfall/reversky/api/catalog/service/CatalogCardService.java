package it.downfall.reversky.api.catalog.service;

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
import it.downfall.reversky.api.catalog.exception.CatalogCardNameAlreadyUsedException;
import it.downfall.reversky.api.catalog.exception.CatalogCardNotFoundException;
import it.downfall.reversky.api.catalog.exception.CatalogCardRulesViolatedException;
import it.downfall.reversky.api.catalog.exception.InvalidCardVariantException;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.CatalogCardSpecifications;
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

	/**
	 * Carte attive, con filtri facoltativi (GET /cards, pubblico).
	 */
	@Transactional(readOnly = true)
	public PageResponse<CatalogCardResponse> findActive(CardType type, String name, Pageable pageable) {

		Specification<CatalogCard> specification = Specification.allOf(
			CatalogCardSpecifications.hasType(type),
			CatalogCardSpecifications.nameContains(name),
			CatalogCardSpecifications.isActive(true)
		);

		Page<CatalogCard> page = catalogCardRepository.findAll(specification, withCardOrder(pageable));
		return PageResponse.from(page.map(CatalogCardResponse::from));
	}

	/**
	 * Tutte le carte, anche disattivate, con filtri facoltativi (GET /admin/cards).
	 *
	 * @param active null = attive e disattivate
	 */
	@Transactional(readOnly = true)
	public PageResponse<CatalogCardResponse> findAll(CardType type, String name, Boolean active, Pageable pageable) {

		Specification<CatalogCard> specification = Specification.allOf(
			CatalogCardSpecifications.hasType(type),
			CatalogCardSpecifications.nameContains(name),
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
	 * Modifica una carta (PUT /admin/cards/{id}).
	 * <p>
	 * Per una variante i campi di gioco della richiesta si ignorano e si copiano dall'originale.
	 * Per un'originale, dopo la modifica, le sue varianti ricevono gli stessi campi di gioco.
	 *
	 * @throws CatalogCardNotFoundException         se la carta non esiste
	 * @throws InvalidCardVariantException          se variantOfId non indica una carta originale valida
	 * @throws CatalogCardRulesViolatedException    se statistiche, sottotipo o effetto non vanno con il tipo
	 * @throws CatalogCardNameAlreadyUsedException  se il nome è di un'altra carta originale
	 */
	@Transactional
	public CatalogCardResponse update(Integer id, CatalogCardUpdateRequest request) {

		CatalogCard card = loadCard(id);
		CatalogCard original = resolveOriginal(card, request.variantOfId());

		if (original == null) {
			validateRules(request.type(), request.subtype(), request.effectCode(), request.atk(), request.def());
		}

		Integer excludedId = (original != null) ? original.getId() : card.getId();
		checkNameAvailable(request.name(), excludedId);

		card.changeVariantOf(original);
		if (original != null) {
			card.copyRulesFrom(original);
		} else {
			card.updateRules(request.type(), request.subtype(), request.effectCode(), request.effectDescription(), request.atk(), request.def(), request.copyLimit());
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

	// Stesse regole dei CHECK di V5, con i metodi di CardType
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

	private void checkNameAvailable(String name, Integer excludedId) {
		if (catalogCardRepository.existsByNameAndVariantOfIsNullAndIdNot(name, excludedId)) {
			throw new CatalogCardNameAlreadyUsedException(name);
		}
	}
}
