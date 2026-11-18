package it.downfall.reversky.api.friendship.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.friendship.entity.Friendship;
import it.downfall.reversky.api.friendship.entity.FriendshipId;
import it.downfall.reversky.api.friendship.entity.FriendshipStatus;

/**
 * Accesso alla tabella {@code friendships} (docs/amicizie).
 * <p>
 * I filtri su {@code status} usano un parametro di tipo {@link FriendshipStatus}, non un literal
 * {@code 'PENDING'}: il valore deve arrivare a PostgreSQL tipato come {@code friendship_status}.
 */
public interface FriendshipRepository extends JpaRepository<Friendship, FriendshipId> {

	/**
	 * Cerca la relazione tra due utenti, in qualunque direzione sia stata creata (docs/amicizie).
	 */
	@Query("""
		SELECT f FROM Friendship f
		WHERE (f.id.requesterId = :a AND f.id.recipientId = :b)
		   OR (f.id.requesterId = :b AND f.id.recipientId = :a)
		""")
	Optional<Friendship> findBetween(@Param("a") Integer a, @Param("b") Integer b);

	/** Amici (ACCEPTED) di un utente, in qualsiasi direzione. */
	@Query("""
		SELECT f FROM Friendship f
		WHERE f.status = :status
		  AND (f.id.requesterId = :userId OR f.id.recipientId = :userId)
		""")
	List<Friendship> findForUserWithStatus(
		@Param("status") FriendshipStatus status,
		@Param("userId") Integer userId);

	/** Richieste PENDING ricevute (dove l'utente è il recipient). */
	@Query("""
		SELECT f FROM Friendship f
		WHERE f.status = :status
		  AND f.id.recipientId = :userId
		ORDER BY f.createdAt DESC
		""")
	List<Friendship> findReceivedWithStatus(
		@Param("status") FriendshipStatus status,
		@Param("userId") Integer userId);

	/** Richieste PENDING inviate (dove l'utente è il requester). */
	@Query("""
		SELECT f FROM Friendship f
		WHERE f.status = :status
		  AND f.id.requesterId = :userId
		ORDER BY f.createdAt DESC
		""")
	List<Friendship> findSentWithStatus(
		@Param("status") FriendshipStatus status,
		@Param("userId") Integer userId);

	/**
	 * Elimina gli inviti di sfida PENDING tra due utenti, in entrambe le direzioni
	 * (docs/amicizie, regola 7). Query nativa finché non esiste l'entità BattleInvite
	 * (arriverà con la task sfide, che la sostituirà con un metodo del proprio service).
	 */
	@Modifying
	@Query(value = """
		DELETE FROM battle_invites
		WHERE status = 'PENDING'
		  AND ((challenger_id = :a AND opponent_id = :b)
		    OR (challenger_id = :b AND opponent_id = :a))
		""", nativeQuery = true)
	int deletePendingBattleInvitesBetween(@Param("a") Integer a, @Param("b") Integer b);
}