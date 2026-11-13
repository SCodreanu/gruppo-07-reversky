package it.downfall.reversky.api.catalog.entity;

/**
 * Sottotipo di una carta (enum PostgreSQL {@code card_subtype}, V1).
 * <p>
 * Solo Spell e Hidden ne hanno uno: quali sono ammessi per ogni tipo lo dice {@link CardType}.
 */
public enum CardSubtype {
	DAMAGE,
	SHIELD,
	BURN,
	SURVIVAL
}
