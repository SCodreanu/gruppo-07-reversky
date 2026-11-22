package it.downfall.reversky.api.battle.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.battle.entity.Battle;
import it.downfall.reversky.api.battle.entity.BattleMode;
import jakarta.persistence.LockModeType;

public interface BattleRepository extends JpaRepository<Battle, UUID> {

	/**
	 * Legge la partita con {@code SELECT ... FOR UPDATE}: due salvataggi dello stesso esito (due retry, o un retry
	 * durante la prima chiamata) si mettono in fila e il secondo trova il risultato già scritto.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from Battle b where b.id = :id")
	public Optional<Battle> findByIdForUpdate(@Param("id") UUID id);

	/** Storico di un utente, dalla partita più recente, senza log né mazzi. */
	@Query(value = """
			select new it.downfall.reversky.api.battle.repository.BattleHistoryRow(b.id, b.createdAt, b.mode,
				b.challengerId, b.opponentId, b.result, b.battleStats, b.challengerRatingDelta, b.opponentRatingDelta)
			from Battle b
			where b.challengerId = :userId or b.opponentId = :userId
			order by b.createdAt desc, b.id
			""",
		countQuery = "select count(b) from Battle b where b.challengerId = :userId or b.opponentId = :userId")
	public Page<BattleHistoryRow> findHistory(@Param("userId") Integer userId, Pageable pageable);

	/** Storico di un utente in una sola modalità. */
	@Query(value = """
			select new it.downfall.reversky.api.battle.repository.BattleHistoryRow(b.id, b.createdAt, b.mode,
				b.challengerId, b.opponentId, b.result, b.battleStats, b.challengerRatingDelta, b.opponentRatingDelta)
			from Battle b
			where (b.challengerId = :userId or b.opponentId = :userId) and b.mode = :mode
			order by b.createdAt desc, b.id
			""",
		countQuery = "select count(b) from Battle b where (b.challengerId = :userId or b.opponentId = :userId) and b.mode = :mode")
	public Page<BattleHistoryRow> findHistoryByMode(@Param("userId") Integer userId, @Param("mode") BattleMode mode,
			Pageable pageable);
}
