package it.downfall.reversky.api.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.downfall.reversky.api.catalog.entity.Rarity;

/**
 * Accesso alla tabella {@code rarities}.
 */
public interface RarityRepository extends JpaRepository<Rarity, Integer> {

	// Elenco per GET /rarities e GET /admin/rarities, dalla più comune alla più rara
	public List<Rarity> findAllByOrderBySortingOrderAsc();

	// Creazione: il nome è già usato?
	public boolean existsByName(String name);

	// Modifica: il nome è già usato da una rarità DIVERSA?
	public boolean existsByNameAndIdNot(String name, Integer id);

	// Creazione: l'ordine è già usato? (in modifica non serve, sortingOrder non cambia)
	public boolean existsBySortingOrder(Integer sortingOrder);
}
