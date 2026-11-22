package it.downfall.reversky.api.deck.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.deck.entity.Deck;

public interface DeckRepository extends JpaRepository<Deck, Integer> {

	/**
	 * I mazzi dell'utente in ordine di nome, poi di id, con le carte e le loro rarità (serviranno alla validazione).
	 */
	@EntityGraph(attributePaths = {"cards", "cards.card", "cards.card.rarity", "cards.card.variantOf",
		"cards.card.variantOf.rarity"})
	public List<Deck> findAllByUserIdOrderByNameAscIdAsc(Integer userId);

	/**
	 * Un mazzo solo se appartiene all'utente: il mazzo di un altro risulta inesistente (404). Carte, rarità, fazioni e
	 * carte originali sono già caricate per la risposta e la validazione.
	 */
	@EntityGraph(attributePaths = {"cards", "cards.card", "cards.card.rarity", "cards.card.faction",
		"cards.card.secondaryFaction", "cards.card.variantOf", "cards.card.variantOf.rarity"})
	public Optional<Deck> findByIdAndUserId(Integer id, Integer userId);

	/** Nome già usato da un altro mazzo dell'utente? Per un mazzo nuovo {@code excludedId} è null. */
	@Query("""
			select count(d) > 0 from Deck d
			where d.userId = :userId and d.name = :name and (:excludedId is null or d.id <> :excludedId)
			""")
	public boolean isNameTaken(@Param("userId") Integer userId, @Param("name") String name,
			@Param("excludedId") Integer excludedId);

	// SQL nativo finché non esiste la coda classificata (API-47, API-48): modificare il mazzo toglie il ticket.
	// Eliminarlo non serve: il database cancella il ticket con ON DELETE CASCADE.
	@Modifying
	@Query(value = "DELETE FROM matchmaking_queue WHERE deck_id = :deckId", nativeQuery = true)
	public int deleteQueueTicket(@Param("deckId") Integer deckId);
}
