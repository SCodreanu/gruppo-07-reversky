package it.downfall.reversky.api.catalog.entity;

import java.util.Set;

/**
 * Tipo di una carta (enum PostgreSQL {@code card_type}, V1).
 * <p>
 * Ogni tipo conosce i sottotipi che ammette: è la stessa regola del vincolo
 * {@code chk_catalog_cards_subtype_matches_type}, controllata prima nel service per rispondere 422
 * invece di un errore del database.
 * <p>
 * Ogni tipo conosce anche le rarità che ammette, come {@code sorting_order} (docs/backoffice,
 * regola 4): Spell e Hidden solo Common o Uncommon, i Terreni solo Rare, le creature qualsiasi.
 * Questa regola non ha un vincolo nel database e la controlla solo il service.
 */
public enum CardType {

	// Primo argomento: sottotipi ammessi. Secondo: sorting_order delle rarità ammesse (vuoto = qualsiasi).
	CREATURE(Set.of(), Set.of()),
	TERRAIN(Set.of(), Set.of(Rarity.RARE_ORDER)),
	SPELL(Set.of(CardSubtype.DAMAGE, CardSubtype.SHIELD, CardSubtype.BURN), Set.of(Rarity.COMMON_ORDER, Rarity.UNCOMMON_ORDER)),
	HIDDEN(Set.of(CardSubtype.SURVIVAL, CardSubtype.DAMAGE), Set.of(Rarity.COMMON_ORDER, Rarity.UNCOMMON_ORDER));

	private final Set<CardSubtype> allowedSubtypes;

	// Ordini (sorting_order) delle rarità ammesse; vuoto = qualsiasi rarità,
	// al contrario di allowedSubtypes dove vuoto = nessun sottotipo
	private final Set<Integer> allowedRarityOrders;

	CardType(Set<CardSubtype> allowedSubtypes, Set<Integer> allowedRarityOrders) {
		this.allowedSubtypes = allowedSubtypes;
		this.allowedRarityOrders = allowedRarityOrders;
	}

	/**
	 * La rarità è ammessa per questo tipo (docs/backoffice, regola 4)?
	 * <p>
	 * Confronta {@code sortingOrder} e mai il nome: il nome si modifica dal backoffice, l'ordine no.
	 * Usa il getter perché {@code rarity} può essere un proxy LAZY di Hibernate.
	 *
	 * @param rarity rarità della carta, già caricata dal service (mai null)
	 * @return true se il tipo ammette qualsiasi rarità oppure se il suo ordine è tra quelli ammessi
	 */
	public boolean acceptsRarity(Rarity rarity) {
		if (allowedRarityOrders.isEmpty()) {
			return true;
		}

		return allowedRarityOrders.contains(rarity.getSortingOrder());
	}

	/**
	 * Il sottotipo è valido per questo tipo? Un sottotipo nullo va bene solo per i tipi che non ne hanno.
	 */
	public boolean acceptsSubtype(CardSubtype subtype) {
		if (subtype == null) {
			return allowedSubtypes.isEmpty();
		}

		return allowedSubtypes.contains(subtype);
	}

	/**
	 * Solo le creature hanno ATK e DEF (chk_catalog_cards_stats_match_type).
	 */
	public boolean hasStats() {
		return this == CREATURE;
	}

	/**
	 * L'effetto è obbligatorio per tutti tranne le creature (chk_catalog_cards_effect_required).
	 */
	public boolean requiresEffect() {
		return this != CREATURE;
	}
}
