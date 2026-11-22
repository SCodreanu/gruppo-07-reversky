package it.downfall.reversky.api.battle.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.battle.entity.MatchmakingTicket;

/**
 * Ticket della coda classificata ({@code matchmaking_queue}).
 */
public interface MatchmakingTicketRepository extends JpaRepository<MatchmakingTicket, Integer> {

	/**
	 * Mette l'utente in coda se non c'è già. Con {@code ON CONFLICT DO NOTHING} due ingressi nello stesso momento non
	 * creano due ticket né falliscono con un errore del database: il secondo non inserisce niente.
	 *
	 * @return 1 se il ticket è stato creato, 0 se l'utente era già in coda
	 */
	@Modifying
	@Query(value = """
			INSERT INTO matchmaking_queue (user_id, deck_id, rating, queued_at)
			VALUES (:userId, :deckId, :rating, :queuedAt)
			ON CONFLICT (user_id) DO NOTHING
			""", nativeQuery = true)
	int insertIfAbsent(@Param("userId") Integer userId, @Param("deckId") Integer deckId, @Param("rating") int rating,
			@Param("queuedAt") Instant queuedAt);

	/**
	 * Gli utenti in coda, dal ticket più vecchio, senza bloccare niente: l'abbinamento (API-49) li prova uno alla volta,
	 * ognuno nella sua transazione.
	 */
	@Query("select t.userId from MatchmakingTicket t order by t.queuedAt, t.userId")
	List<Integer> findUserIdsInQueueOrder();

	/**
	 * Blocca il ticket dell'utente con {@code FOR UPDATE SKIP LOCKED}: se un'altra esecuzione dell'abbinamento lo tiene
	 * già, non si aspetta e il risultato è vuoto, come per un ticket che non c'è più.
	 */
	@Query(value = "SELECT * FROM matchmaking_queue WHERE user_id = :userId FOR UPDATE SKIP LOCKED", nativeQuery = true)
	Optional<MatchmakingTicket> lockIfFree(@Param("userId") Integer userId);

	/**
	 * Blocca l'avversario per un ticket (docs/sfide, "Abbinamento" e "Anti-boosting"): un altro utente con il rating
	 * nella finestra, prima il più vicino, poi il più vecchio in coda, saltando chi ha giocato con me una classificata
	 * preparata dopo {@code since}. Con {@code SKIP LOCKED} un ticket bloccato da un'altra esecuzione si salta.
	 *
	 * @param userId     l'utente del ticket per cui si cerca
	 * @param rating     il rating salvato nel suo ticket
	 * @param minRating  limite basso della finestra, compreso
	 * @param maxRating  limite alto della finestra, compreso
	 * @param since      le classificate tra i due preparate dopo questo momento escludono l'avversario
	 */
	@Query(value = """
			SELECT q.* FROM matchmaking_queue q
			WHERE q.user_id <> :userId
			  AND q.rating BETWEEN :minRating AND :maxRating
			  AND NOT EXISTS (
			      SELECT 1 FROM battles b
			      WHERE b.mode = 'RANKED'
			        AND b.created_at > :since
			        AND ((b.challenger_id = :userId AND b.opponent_id = q.user_id)
			          OR (b.challenger_id = q.user_id AND b.opponent_id = :userId))
			  )
			ORDER BY abs(q.rating - :rating), q.queued_at, q.user_id
			LIMIT 1
			FOR UPDATE OF q SKIP LOCKED
			""", nativeQuery = true)
	Optional<MatchmakingTicket> lockOpponent(@Param("userId") Integer userId, @Param("rating") int rating,
			@Param("minRating") int minRating, @Param("maxRating") int maxRating, @Param("since") Instant since);

	/** Toglie il ticket dell'utente; 0 se non era in coda. */
	@Modifying
	@Query("delete from MatchmakingTicket t where t.userId = :userId")
	int deleteByUserId(@Param("userId") Integer userId);

	/** Toglie il ticket in coda con quel mazzo; 0 se il mazzo non era in coda. */
	@Modifying
	@Query("delete from MatchmakingTicket t where t.deckId = :deckId")
	int deleteByDeckId(@Param("deckId") Integer deckId);
}
