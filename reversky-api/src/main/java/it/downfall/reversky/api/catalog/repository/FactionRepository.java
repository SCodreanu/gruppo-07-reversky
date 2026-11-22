package it.downfall.reversky.api.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.downfall.reversky.api.catalog.entity.Faction;

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
}
