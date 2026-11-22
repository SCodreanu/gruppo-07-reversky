package it.downfall.reversky.api.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import it.downfall.reversky.api.catalog.entity.CatalogCard;

/**
 * Accesso alla tabella {@code catalog_cards}.
 * <p>
 * Le ricerche con filtri (GET /cards, GET /admin/cards) usano {@link CatalogCardSpecifications}
 * con {@code findAll(Specification, Pageable)} di JpaSpecificationExecutor.
 */
public interface CatalogCardRepository extends JpaRepository<CatalogCard, Integer>, JpaSpecificationExecutor<CatalogCard> {

	// Nome già usato da una carta ORIGINALE diversa da excludedId?
	// Per un'originale excludedId è il suo id, per una variante l'id della sua originale.
	public boolean existsByNameAndVariantOfIsNullAndIdNot(String name, Integer excludedId);

	// Varianti di un'originale, per copiare le modifiche dell'originale
	public List<CatalogCard> findAllByVariantOf(CatalogCard original);

	// L'originale ha almeno una variante? Allora non può diventare a sua volta variante
	public boolean existsByVariantOf(CatalogCard original);
}
