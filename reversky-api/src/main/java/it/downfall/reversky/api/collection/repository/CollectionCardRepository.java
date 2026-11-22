package it.downfall.reversky.api.collection.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.collection.entity.CollectionCard;
import it.downfall.reversky.api.collection.entity.CollectionCardId;

/**
 * Righe di {@code collection_cards}. Le query su mazzi e scambi sono SQL nativo finché non esistono le entità
 * {@code Deck} (API-26) e {@code Trade} (API-31): poi diventano query JPA, con lo stesso risultato.
 */
public interface CollectionCardRepository
		extends JpaRepository<CollectionCard, CollectionCardId>, JpaSpecificationExecutor<CollectionCard> {

	/** Le righe dell'utente per quelle carte; quelle che non possiede mancano. */
	List<CollectionCard> findByIdUserIdAndIdCardIdIn(Integer userId, Collection<Integer> cardIds);

	/**
	 * Una pagina di righe con la carta, la sua rarità e le sue fazioni già caricate: la risposta le usa tutte.
	 */
	@Override
	@EntityGraph(attributePaths = {"card", "card.rarity", "card.faction", "card.secondaryFaction"})
	Page<CollectionCard> findAll(Specification<CollectionCard> specification, Pageable pageable);

	/** Per ogni carta, in quanti mazzi dell'utente compare (docs/collezione, {@code usedInDecks}). */
	@Query(value = """
			SELECT dc.card_id AS "cardId", COUNT(*) AS "total"
			FROM deck_cards dc
			JOIN decks d ON d.id = dc.deck_id
			WHERE d.user_id = :userId AND dc.card_id IN (:cardIds)
			GROUP BY dc.card_id
			""", nativeQuery = true)
	List<CardCount> countDecksUsing(@Param("userId") Integer userId, @Param("cardIds") Collection<Integer> cardIds);

	/**
	 * Per ogni carta, le copie riservate dalle offerte aperte dell'utente (docs/scambi, "Copie riservate"): conta solo
	 * l'offerta di chi ha fatto la proposta in corso, cioè il richiedente con {@code WAITING_RECIPIENT} e il
	 * destinatario con {@code WAITING_REQUESTER}.
	 */
	@Query(value = """
			SELECT o.card_id AS "cardId", SUM(o.quantity) AS "total"
			FROM trade_offers o
			JOIN trades t ON t.id = o.trade_id
			WHERE o.user_id = :userId AND o.card_id IN (:cardIds)
			  AND ((t.status = 'WAITING_RECIPIENT' AND t.requester_id = :userId)
			    OR (t.status = 'WAITING_REQUESTER' AND t.recipient_id = :userId))
			GROUP BY o.card_id
			""", nativeQuery = true)
	List<CardCount> countReserved(@Param("userId") Integer userId, @Param("cardIds") Collection<Integer> cardIds);

	/**
	 * Avanzamento della collezione per rarità (GET /collection/summary, deciso il 20 novembre): {@code total} sono le
	 * carte originali attive della rarità, {@code owned} quelle che l'utente possiede direttamente o con una loro
	 * variante (Q-08). Le rarità senza originali attivi non compaiono.
	 */
	@Query(value = """
			SELECT r.id AS "rarityId", r.name AS "rarityName", COUNT(c.id) AS "total",
			       COUNT(c.id) FILTER (WHERE EXISTS (
			           SELECT 1 FROM collection_cards cc
			           JOIN catalog_cards owned ON owned.id = cc.card_id
			           WHERE cc.user_id = :userId AND (owned.id = c.id OR owned.variant_of_id = c.id))) AS "owned"
			FROM rarities r
			JOIN catalog_cards c ON c.rarity_id = r.id AND c.variant_of_id IS NULL AND c.is_active
			GROUP BY r.id, r.name, r.sorting_order
			ORDER BY r.sorting_order
			""", nativeQuery = true)
	List<RarityProgressRow> summarize(@Param("userId") Integer userId);
}
