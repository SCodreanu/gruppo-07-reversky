package it.downfall.reversky.api.ruleset.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.ruleset.entity.RuleSet;
import jakarta.persistence.LockModeType;

/**
 * Accesso alla tabella {@code rule_sets}.
 */
public interface RuleSetRepository extends JpaRepository<RuleSet, Integer> {

	// Elenco per GET /admin/rule-sets: la versione più recente per prima
	public List<RuleSet> findAllByOrderByIdDesc();

	// Versione attiva (GET /rules)
	public Optional<RuleSet> findByIsActiveTrue();

	// Attivazione: blocca la versione attiva, così due attivazioni contemporanee si mettono in fila
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT r FROM RuleSet r WHERE r.isActive = true")
	public Optional<RuleSet> findActiveForUpdate();

	// Creazione: la versione è già usata?
	public boolean existsByVersion(String version);

	// Modifica: la versione è già usata da un rule set DIVERSO?
	public boolean existsByVersionAndIdNot(String version, Integer id);

	// Una versione già usata da una partita non si modifica (docs/backoffice).
	// Query nativa perché l'entità Battle non esiste ancora: quando arriverà battle/, il controllo
	// passerà a un metodo del suo service (CodingRequirements §4, niente repository di altre funzionalità).
	@Query(value = "SELECT EXISTS (SELECT 1 FROM battles WHERE rule_set_id = :id)", nativeQuery = true)
	public boolean isUsedByBattle(@Param("id") Integer id);
}
