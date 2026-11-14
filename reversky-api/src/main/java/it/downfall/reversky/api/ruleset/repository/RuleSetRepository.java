package it.downfall.reversky.api.ruleset.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.ruleset.entity.RuleSet;

/**
 * Accesso alla tabella {@code rule_sets}.
 */
public interface RuleSetRepository extends JpaRepository<RuleSet, Integer> {

	/** Elenco per GET /admin/rule-sets: più recenti per primi. */
	List<RuleSet> findAllByOrderByIdDesc();

	/** Regola attiva corrente (GET /rules). */
	Optional<RuleSet> findByIsActiveTrue();

	/** Creazione: la versione è già usata? */
	boolean existsByVersion(String version);

	/** Modifica: la versione è già usata da un rule set DIVERSO? */
	boolean existsByVersionAndIdNot(String version, Integer id);

	/**
	 * Disattiva tutti gli eventuali attivi diversi da {@code id}. Così quando
	 * {@code toActivate.activate()} viene flushato, l'indice parziale non è violato.
	 */
	@Modifying(flushAutomatically = true)
	@Query("UPDATE RuleSet r SET r.isActive = false WHERE r.isActive = true AND r.id <> :id")
	void deactivateAllActiveExcept(@Param("id") Integer id);

	/** Un rule set già usato da una partita non si può più modificare (docs/backoffice). */
	@Query(value = "SELECT COUNT(*) > 0 FROM battles WHERE rule_set_id = :id", nativeQuery = true)
	boolean isUsedByBattle(@Param("id") Integer id);
}