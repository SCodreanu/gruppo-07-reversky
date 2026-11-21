package it.downfall.reversky.engine.web.dto;

import it.downfall.reversky.engine.core.model.CardType;

/**
 * Un effetto implementato, in {@code GET /api/v1/effects} (docs/api-engine): il backoffice dell'API lo usa per sapere
 * quali {@code effect_code} può assegnare e a quale tipo di carta.
 *
 * @param code      codice dell'effetto, uguale a {@code catalog_cards.effect_code}
 * @param appliesTo tipo di carta che può portarlo
 */
public record EffectResponse(String code, CardType appliesTo) {
}
