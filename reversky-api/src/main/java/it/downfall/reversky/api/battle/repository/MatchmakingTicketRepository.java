package it.downfall.reversky.api.battle.repository;

import java.time.Instant;

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

	/** Toglie il ticket dell'utente; 0 se non era in coda. */
	@Modifying
	@Query("delete from MatchmakingTicket t where t.userId = :userId")
	int deleteByUserId(@Param("userId") Integer userId);

	/** Toglie il ticket in coda con quel mazzo; 0 se il mazzo non era in coda. */
	@Modifying
	@Query("delete from MatchmakingTicket t where t.deckId = :deckId")
	int deleteByDeckId(@Param("deckId") Integer deckId);
}
