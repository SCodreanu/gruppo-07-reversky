package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.Wound;

/**
 * Wretches, Uncommon (Theory of the Wound), <i>Furia del ferito</i>: "la prima volta che viene ferito, +2 ATK finché
 * resta in campo".
 * <p>
 * Ferito = una perdita reale di DEF sopra 0, dopo gli scudi, da un attacco o da un effetto di qualunque origine
 * (revisione del 10 novembre). Una volta per entrata in campo (D-31); se la ferita arriva all'entrata, il bonus vale
 * già nel primo round.
 */
public final class WretchesEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "WRETCHES";

	private static final int ATTACK_BONUS = 2;

	private boolean used;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onWounded(CreatureContext ctx, Wound wound) {
		if (used) {
			return;
		}
		used = true;
		EffectActions.log(ctx, List.of(EffectActions.attack(ctx.self(), ctx.self(), ATTACK_BONUS,
				ModifierDuration.whileInPlay(), "while in play")));
	}
}
