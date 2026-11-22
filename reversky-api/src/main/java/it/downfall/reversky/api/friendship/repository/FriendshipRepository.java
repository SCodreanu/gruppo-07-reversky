package it.downfall.reversky.api.friendship.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
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
	// TODO(@acistar0): la query non ha ORDER BY, quindi GET /friends può restituire gli amici in un
	// ordine diverso a ogni chiamata. Aggiungere un ordine fisso: il più semplice è
	// "ORDER BY f.createdAt DESC, f.id.requesterId, f.id.recipientId" (come le richieste qui sotto,
	// con gli id come spareggio). Se si preferisce l'ordine per username, ordinare nel service dopo
	// aver caricato gli utenti (vedi il TODO in FriendshipService.findFriends).
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
}