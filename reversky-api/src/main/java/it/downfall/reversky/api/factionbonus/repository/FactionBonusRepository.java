package it.downfall.reversky.api.factionbonus.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.downfall.reversky.api.factionbonus.entity.FactionBonus;
import it.downfall.reversky.api.factionbonus.entity.FactionBonusId;

/**
 * Accesso alla tabella {@code faction_bonuses}.
 * <p>
 * L'ordine si scrive in JPQL esplicita: {@code originFactionId} e {@code targetFactionId}
 * stanno dentro l'embedded {@code id}, quindi una derived query {@code findAllByOrderByOrigin...}
 * non li troverebbe (Hibernate cerca property top-level).
 */
public interface FactionBonusRepository extends JpaRepository<FactionBonus, FactionBonusId> {

	/** Elenco stabile per GET /admin/faction-bonuses e per GET /rules. */
	@Query("SELECT fb FROM FactionBonus fb ORDER BY fb.id.originFactionId ASC, fb.id.targetFactionId ASC")
	List<FactionBonus> findAllOrdered();
}