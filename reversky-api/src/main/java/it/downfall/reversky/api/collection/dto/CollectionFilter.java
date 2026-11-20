package it.downfall.reversky.api.collection.dto;

import it.downfall.reversky.api.catalog.entity.CardType;

/**
 * Filtri facoltativi della collezione, gli stessi di GET /cards (docs/collezione): ogni campo null non filtra.
 *
 * @param type      solo un tipo di carta
 * @param name      nome che contiene il testo, senza maiuscole
 * @param rarityId  solo una rarità
 * @param factionId solo una fazione, prima o seconda
 */
public record CollectionFilter(CardType type, String name, Integer rarityId, Integer factionId) {
}
