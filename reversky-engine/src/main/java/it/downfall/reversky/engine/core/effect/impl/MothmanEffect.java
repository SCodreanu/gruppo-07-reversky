package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;

/**
 * Mothman, Mythic (Theory of the Invitation), <i>Disastro annunciato</i>: "all'entrata, e ogni volta che una creatura
 * avversaria entra mentre Mothman è in campo, quella creatura subisce 3 danni".
 * <p>
 * Ogni scontro mette Mothman davanti a una nuova avversaria: il danno arriva a ogni inizio di scontro
 * ({@code onCombatStart}, deciso il 18 novembre), quindi una volta per avversaria.
 */
public final class MothmanEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "MOTHMAN";

	private static final int DAMAGE = 3;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onCombatStart(CreatureContext ctx) {
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> parts.add(EffectActions.damage(enemy, DAMAGE)));
		EffectActions.log(ctx, parts);
	}
}
