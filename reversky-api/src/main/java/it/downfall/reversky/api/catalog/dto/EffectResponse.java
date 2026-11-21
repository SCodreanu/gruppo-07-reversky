package it.downfall.reversky.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.Effect;

/**
 * Un effetto assegnabile a una carta, in GET /admin/effects (docs/backoffice, regola 2).
 *
 * @param code      il valore da mettere in {@code effectCode} della carta
 * @param appliesTo l'unico tipo di carta che può portarlo
 */
public record EffectResponse(

	@Schema(example = "VEIL_OF_ANCHORED")
	String code,

	@Schema(example = "SPELL")
	CardType appliesTo
) {

	public static EffectResponse from(Effect effect) {
		return new EffectResponse(effect.getCode(), effect.getAppliesTo());
	}
}
