package it.downfall.reversky.api.battle.entity;

/**
 * Una carta del mazzo con i valori del catalogo al momento della preparazione (fotografia in
 * {@code battles.challenger_deck} e {@code opponent_deck}, docs/sfide): la partita resta identica nello storico e nel
 * retry anche se il backoffice cambia la carta.
 *
 * @param quantity copie nel mazzo; la richiesta all'engine ha una voce per copia
 */
public record DeckCardSnapshot(
	Integer cardId,
	String name,
	String type,
	String rarity,
	int rarityOrder,
	Integer factionId,
	Integer secondaryFactionId,
	Integer atk,
	Integer def,
	String subtype,
	String effectCode,
	int quantity
) {
}
