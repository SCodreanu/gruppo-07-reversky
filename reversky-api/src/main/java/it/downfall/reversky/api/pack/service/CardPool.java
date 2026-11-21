package it.downfall.reversky.api.pack.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.entity.Rarity;

/**
 * Le carte che possono uscire da un pacchetto, divise per rarità (docs/pacchetti). Senza database: si costruisce con
 * le carte attive e originali del catalogo.
 * <p>
 * Le rarità si confrontano per {@code sorting_order}, mai per nome. Finché non esiste il filtro dei tag (API-21) ogni
 * pacchetto pesca da tutte le carte attive e originali.
 */
public final class CardPool {

	private final NavigableMap<Integer, List<CatalogCard>> cardsByOrder;

	public CardPool(Collection<CatalogCard> cards) {
		this.cardsByOrder = cards.stream()
			.filter(card -> card.isActive() && !card.isVariant())
			.collect(Collectors.groupingBy(card -> card.getRarity().getSortingOrder(), TreeMap::new, Collectors.toList()));
	}

	/** La rarità ha almeno una carta? Un bonus di una rarità senza carte non scatta e GET /packs non lo mostra. */
	public boolean hasCards(Rarity rarity) {
		return !cardsByOrder.getOrDefault(rarity.getSortingOrder(), List.of()).isEmpty();
	}

	/**
	 * Le carte tra cui scegliere per uno slot della rarità indicata. Se la rarità non ha carte, ripiega sulla rarità
	 * inferiore più vicina che ne ha e, se sotto non c'è niente, su quella superiore più vicina. Il ripiego non sceglie
	 * mai un Terreno né un Oggetto, che escono solo dagli slot della loro rarità.
	 *
	 * @return le carte candidate, lista vuota se non c'è nessuna carta neanche con il ripiego
	 */
	public List<CatalogCard> candidatesFor(Rarity rarity) {
		int order = rarity.getSortingOrder();
		List<CatalogCard> exact = cardsByOrder.getOrDefault(order, List.of());
		if (!exact.isEmpty()) {
			return exact;
		}

		for (Map.Entry<Integer, List<CatalogCard>> lower : cardsByOrder.headMap(order, false).descendingMap().entrySet()) {
			List<CatalogCard> fallback = withoutTerrainsAndItems(lower.getValue());
			if (!fallback.isEmpty()) {
				return fallback;
			}
		}
		for (Map.Entry<Integer, List<CatalogCard>> higher : cardsByOrder.tailMap(order, false).entrySet()) {
			List<CatalogCard> fallback = withoutTerrainsAndItems(higher.getValue());
			if (!fallback.isEmpty()) {
				return fallback;
			}
		}
		return List.of();
	}

	private static List<CatalogCard> withoutTerrainsAndItems(List<CatalogCard> cards) {
		return cards.stream()
			.filter(card -> card.getCardType() != CardType.TERRAIN && card.getCardType() != CardType.ITEM)
			.toList();
	}
}
