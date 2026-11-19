package it.downfall.reversky.api.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import it.downfall.reversky.api.catalog.entity.FactionBonus;
import it.downfall.reversky.api.catalog.entity.FactionBonusId;

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

	/**
	 * Blocca la tabella fino alla fine della transazione, per PUT /admin/faction-bonuses.
	 * <p>
	 * {@code SHARE ROW EXCLUSIVE} è in conflitto con se stesso e con le scritture, ma non con le
	 * letture: due sostituzioni contemporanee si mettono in fila (vince l'ultima), mentre GET /rules
	 * e GET /admin/faction-bonuses continuano a leggere la matrice precedente finché la sostituzione
	 * non è confermata. Senza il blocco la seconda transazione non vedrebbe le righe inserite dalla
	 * prima e il risultato sarebbe un misto delle due matrici, o un errore di chiave duplicata.
	 */
	@Modifying
	@Query(value = "LOCK TABLE faction_bonuses IN SHARE ROW EXCLUSIVE MODE", nativeQuery = true)
	void lockForReplace();
}
