package it.downfall.reversky.api.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.catalog.entity.Faction;
import jakarta.persistence.LockModeType;

/**
 * Accesso alla tabella {@code factions}.
 */
public interface FactionRepository extends JpaRepository<Faction, Integer> {

	// Elenco per GET /factions e GET /admin/factions, nell'ordine di inserimento
	public List<Faction> findAllByOrderByIdAsc();

	// Creazione: il nome è già usato da un'altra fazione?
	public boolean existsByName(String name);

	// Modifica: il nome è già usato da una fazione DIVERSA da quella che sto modificando?
	public boolean existsByNameAndIdNot(String name, Integer id);

	// GET /admin/factions: con i tag in una sola query, invece di una per fazione
	@EntityGraph(attributePaths = "tags")
	public List<Faction> findWithTagsByOrderByIdAsc();

	// PUT /admin/factions/{id}/tags: due sostituzioni contemporanee dei tag della stessa fazione si mettono in fila,
	// invece di cancellare e reinserire le stesse righe di faction_tags insieme
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select f from Faction f where f.id = :id")
	public Optional<Faction> findByIdForUpdate(@Param("id") Integer id);
}
