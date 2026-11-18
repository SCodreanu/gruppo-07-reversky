package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * Wampus Cat, Uncommon (Theory of the Invitation), <i>Sguardo della follia</i>: "all'entrata avv. −1 ATK finché resta
 * in campo". Il malus resta sulla creatura che l'ha ricevuto (D-35).
 */
public final class WampusCatEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "WAMPUS_CAT";

	private static final int PENALTY = 1;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> parts.add(EffectActions.attack(enemy, ctx.self(), -PENALTY,
				ModifierDuration.whileInPlay(), "while in play")));
		EffectActions.log(ctx, parts);
	}
}
