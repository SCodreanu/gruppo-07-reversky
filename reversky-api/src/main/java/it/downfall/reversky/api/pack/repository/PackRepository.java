package it.downfall.reversky.api.pack.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.pack.entity.Pack;

public interface PackRepository extends JpaRepository<Pack, Integer> {

	/** I pacchetti del negozio, in ordine di id. Slot e bonus arrivano con una query ciascuno (SUBSELECT). */
	public List<Pack> findAllByActiveTrueOrderByIdAsc();

	/** Tutti i pacchetti per il backoffice, anche disattivati. */
	public List<Pack> findAllByOrderByIdAsc();

	/** Un pacchetto attivo, per l'acquisto. */
	public Optional<Pack> findByIdAndActiveTrue(Integer id);

	/** Nome già usato da un altro pacchetto? Per un pacchetto nuovo {@code excludedId} è null. */
	@Query("select count(p) > 0 from Pack p where p.name = :name and (:excludedId is null or p.id <> :excludedId)")
	public boolean isNameTaken(@Param("name") String name, @Param("excludedId") Integer excludedId);
}
