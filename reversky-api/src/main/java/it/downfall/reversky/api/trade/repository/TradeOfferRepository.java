package it.downfall.reversky.api.trade.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import it.downfall.reversky.api.trade.entity.TradeOffer;
import it.downfall.reversky.api.trade.entity.TradeOfferId;

public interface TradeOfferRepository extends JpaRepository<TradeOffer, TradeOfferId> {

	/**
	 * Le offerte di più scambi in una query, con la carta, la rarità e le fazioni già caricate: servono a
	 * {@code CatalogCardResponse} per una pagina intera di scambi.
	 */
	@EntityGraph(attributePaths = {"card", "card.rarity", "card.faction", "card.secondaryFaction", "card.variantOf"})
	public List<TradeOffer> findAllByIdTradeIdIn(Collection<UUID> tradeIds);
}
