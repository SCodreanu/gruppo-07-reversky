package it.downfall.reversky.api.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.downfall.reversky.api.catalog.entity.Tag;

/**
 * Accesso alla tabella {@code tags}.
 */
public interface TagRepository extends JpaRepository<Tag, Integer> {

	// Elenco per GET /admin/tags, in ordine alfabetico
	public List<Tag> findAllByOrderByNameAsc();

	// Creazione: il nome è già usato?
	public boolean existsByName(String name);

	// Modifica: il nome è già usato da un tag DIVERSO da quello che sto modificando?
	public boolean existsByNameAndIdNot(String name, Integer id);
}
