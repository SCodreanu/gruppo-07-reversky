package it.downfall.reversky.api.catalog.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.catalog.entity.CatalogCard;
import jakarta.persistence.LockModeType;

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

	// Nome già usato da una carta ORIGINALE, per una carta nuova (POST /admin/cards). Il metodo con IdNot non va bene:
	// con excludedId null diventerebbe "id <> NULL", che non è mai vero, e il nome risulterebbe sempre libero.
	public boolean existsByNameAndVariantOfIsNull(String name);

	// Varianti di un'originale, per copiare le modifiche dell'originale
	public List<CatalogCard> findAllByVariantOf(CatalogCard original);

	/**
	 * Le carte con quegli id, con rarità, fazioni e carta originale (e la sua rarità) già caricate: servono ai mazzi
	 * per la risposta e per i limiti di copie. Gli id che non esistono mancano.
	 */
	@EntityGraph(attributePaths = {"rarity", "faction", "secondaryFaction", "variantOf", "variantOf.rarity"})
	public List<CatalogCard> findAllByIdIn(Collection<Integer> ids);

	/** Le carte attive e originali con rarità e fazioni: tra queste si pescano le carte dei pacchetti. */
	@EntityGraph(attributePaths = {"rarity", "faction", "secondaryFaction"})
	public List<CatalogCard> findAllByIsActiveTrueAndVariantOfIsNullOrderByIdAsc();

	// L'originale ha almeno una variante? Allora non può diventare a sua volta variante
	public boolean existsByVariantOf(CatalogCard original);

	// PUT /admin/cards/{id}/tags: due sostituzioni contemporanee dei tag della stessa carta si mettono in fila,
	// invece di cancellare e reinserire le stesse righe di card_tags insieme (API-12)
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from CatalogCard c where c.id = :id")
	public Optional<CatalogCard> findByIdForUpdate(@Param("id") Integer id);
}
