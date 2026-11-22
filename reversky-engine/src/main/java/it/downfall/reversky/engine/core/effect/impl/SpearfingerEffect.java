package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * Spearfinger, Rare (Theory of the Invitation): "il primo attacco contro ogni nuova creatura avversaria fa +2".
 * <p>
 * È un bonus ATK (catalogo): a ogni nuovo scontro +2 ATK per un round (deciso il 18 novembre). Se quel round si
 * interrompe prima degli attacchi, il bonus si perde.
 */
public final class SpearfingerEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SPEARFINGER";

	private static final int ATTACK_BONUS = 2;

	/** Crea l'effetto. */
	public SpearfingerEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onCombatStart(CreatureContext ctx) {
		String against = ctx.enemy().map(enemy -> "on its first attack against " + enemy.name()).orElse("");
		EffectActions.log(ctx, List.of(EffectActions.attack(ctx.self(), ctx.self(), ATTACK_BONUS,
				ModifierDuration.rounds(1), against)));
	}
}
