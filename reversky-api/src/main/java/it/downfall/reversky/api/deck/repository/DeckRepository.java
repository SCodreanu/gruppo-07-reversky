package it.downfall.reversky.api.deck.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.deck.entity.Deck;

public interface DeckRepository extends JpaRepository<Deck, Integer> {

	/** I mazzi dell'utente in ordine di nome, poi di id. */
	public List<Deck> findAllByUserIdOrderByNameAscIdAsc(Integer userId);

	/** Un mazzo solo se appartiene all'utente: il mazzo di un altro risulta inesistente (404). */
	public Optional<Deck> findByIdAndUserId(Integer id, Integer userId);

	/** Nome già usato da un altro mazzo dell'utente? Per un mazzo nuovo {@code excludedId} è null. */
	@Query("""
			select count(d) > 0 from Deck d
			where d.userId = :userId and d.name = :name and (:excludedId is null or d.id <> :excludedId)
			""")
	public boolean isNameTaken(@Param("userId") Integer userId, @Param("name") String name,
			@Param("excludedId") Integer excludedId);

	// SQL nativo finché non esiste battle/ (API-37, API-39): poi passerà a un metodo del suo service
	@Query(value = "SELECT EXISTS (SELECT 1 FROM battle_invites WHERE challenger_deck_id = :deckId AND status = 'PENDING')",
			nativeQuery = true)
	public boolean isUsedByPendingInvite(@Param("deckId") Integer deckId);

	// SQL nativo finché non esiste la coda classificata (API-47, API-48): modificare il mazzo toglie il ticket.
	// Eliminarlo non serve: il database cancella il ticket con ON DELETE CASCADE.
	@Modifying
	@Query(value = "DELETE FROM matchmaking_queue WHERE deck_id = :deckId", nativeQuery = true)
	public int deleteQueueTicket(@Param("deckId") Integer deckId);
}
