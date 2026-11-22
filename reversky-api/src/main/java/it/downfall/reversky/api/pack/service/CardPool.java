package it.downfall.reversky.api.pack.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.entity.Faction;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.catalog.entity.Tag;

/**
 * Le carte che possono uscire da un pacchetto, divise per rarità (docs/pacchetti). Senza database: si costruisce con
 * le carte attive del catalogo, varianti comprese, e i tag del pacchetto.
 * <p>
 * Filtro dei tag (API-21, docs/pacchetti, "Filtro delle carte"):
 * <ul>
 * <li>pacchetto senza tag: qualsiasi carta attiva, tranne le varianti;</li>
 * <li>pacchetto con tag: una carta che ha almeno uno dei tag tra i propri o tra quelli di una delle sue fazioni, anche
 * la seconda;</li>
 * <li>una variante esce solo con uno dei tag del pacchetto tra i <b>propri</b>: non bastano quelli delle fazioni, così
 * una versione evento esce solo dal pacchetto del suo evento;</li>
 * <li>Spell e Hidden non hanno fazione: con i tag escono solo tramite i propri.</li>
 * </ul>
 * I tag si confrontano per nome, che è univoco ({@code uq_tags_name}). Le rarità si confrontano per
 * {@code sorting_order}, mai per nome.
 */
public final class CardPool {

	private final NavigableMap<Integer, List<CatalogCard>> cardsByOrder;

	/** Le carte di un pacchetto senza tag: tutte le attive e originali. */
	public CardPool(Collection<CatalogCard> cards) {
		this(cards, Set.of());
	}

	/**
	 * @param cards    le carte attive del catalogo, varianti comprese, con i tag propri e delle fazioni caricati
	 * @param packTags i nomi dei tag del pacchetto; vuoto per un pacchetto senza tag
	 */
	public CardPool(Collection<CatalogCard> cards, Set<String> packTags) {
		this.cardsByOrder = cards.stream()
			.filter(card -> card.isActive() && canDrop(card, packTags))
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

	// La regola dei tag di docs/pacchetti: senza tag solo originali; con i tag i propri valgono per tutte, quelli delle
	// fazioni solo per le originali (Spell e Hidden non hanno fazione, quindi per loro contano solo i propri)
	private static boolean canDrop(CatalogCard card, Set<String> packTags) {
		if (packTags.isEmpty()) {
			return !card.isVariant();
		}
		if (hasAny(card.getTags(), packTags)) {
			return true;
		}
		return !card.isVariant()
			&& (hasFactionTag(card.getFaction(), packTags) || hasFactionTag(card.getSecondaryFaction(), packTags));
	}

	private static boolean hasFactionTag(Faction faction, Set<String> packTags) {
		return faction != null && hasAny(faction.getTags(), packTags);
	}

	private static boolean hasAny(Set<Tag> tags, Set<String> packTags) {
		return tags.stream().anyMatch(tag -> packTags.contains(tag.getName()));
	}

	private static List<CatalogCard> withoutTerrainsAndItems(List<CatalogCard> cards) {
		return cards.stream()
			.filter(card -> card.getCardType() != CardType.TERRAIN && card.getCardType() != CardType.ITEM)
			.toList();
	}
}
