package it.downfall.reversky.api.catalog.entity;

import java.util.Set;

/**
 * Tipo di una carta (enum PostgreSQL {@code card_type}, V1).
 * <p>
 * Ogni tipo conosce i sottotipi che ammette: è la stessa regola del vincolo
 * {@code chk_catalog_cards_subtype_matches_type}, controllata prima nel service per rispondere 422
 * invece di un errore del database.
 */
public enum CardType {

	CREATURE(Set.of()),
	TERRAIN(Set.of()),
	SPELL(Set.of(CardSubtype.DAMAGE, CardSubtype.SHIELD, CardSubtype.BURN)),
	HIDDEN(Set.of(CardSubtype.SURVIVAL, CardSubtype.DAMAGE));

	private final Set<CardSubtype> allowedSubtypes;

	CardType(Set<CardSubtype> allowedSubtypes) {
		this.allowedSubtypes = allowedSubtypes;
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
