package it.downfall.reversky.api.battle.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.battle.entity.BattleInvite;
import it.downfall.reversky.api.battle.entity.BattleInviteStatus;
import jakarta.persistence.LockModeType;

public interface BattleInviteRepository extends JpaRepository<BattleInvite, UUID> {

	/** Inviti ricevuti in un certo stato, dal più recente. */
	public List<BattleInvite> findAllByOpponentIdAndStatusOrderByCreatedAtDesc(Integer opponentId, BattleInviteStatus status);

	/** Inviti inviati in un certo stato, dal più recente. */
	public List<BattleInvite> findAllByChallengerIdAndStatusOrderByCreatedAtDesc(Integer challengerId, BattleInviteStatus status);

	/** Esiste già un invito in quello stato dallo sfidante all'avversario (uq_battle_invites_pending_pair)? */
	public boolean existsByChallengerIdAndOpponentIdAndStatus(Integer challengerId, Integer opponentId,
			BattleInviteStatus status);

	/**
	 * Legge l'invito con {@code SELECT ... FOR UPDATE}: due accettazioni contemporanee non creano due partite.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from BattleInvite i where i.id = :id")
	public Optional<BattleInvite> findByIdForUpdate(@Param("id") UUID id);

	/** Il mazzo è quello dello sfidante in un invito in quello stato (idx_battle_invites_challenger_deck_id)? */
	public boolean existsByChallengerDeckIdAndStatus(Integer challengerDeckId, BattleInviteStatus status);

	/**
	 * Elimina gli inviti in quello stato tra due utenti, in entrambe le direzioni. È un {@code DELETE} unico, senza
	 * caricare le entità.
	 *
	 * @return quanti inviti ha eliminato
	 */
	@Modifying
	@Query("""
		delete from BattleInvite i
		where i.status = :status
		  and ((i.challengerId = :a and i.opponentId = :b) or (i.challengerId = :b and i.opponentId = :a))
		""")
	public int deleteBetween(@Param("a") Integer a, @Param("b") Integer b, @Param("status") BattleInviteStatus status);
}
