package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.StatModifier;

/**
 * Base degli effetti "all'entrata avv. −2 ATK nel primo round e −1 nel secondo" (classi base, D-37): SCP-049 e poi
 * Howlers, Noppera-bō, The Masked Bloom. Due modificatori sull'avversaria (D-35): −1 per due round e −1 per un round.
 */
public abstract class FadingAttackPenaltyEffect implements CreatureEffect {

	/** Crea la base. */
	protected FadingAttackPenaltyEffect() {
	}

	@Override
	public final void onEnterPlay(CreatureContext ctx) {
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> {
			enemy.addModifier(StatModifier.attack(ModifierSource.EFFECT, ctx.self().side(), -1,
					ModifierDuration.rounds(2)));
			enemy.addModifier(StatModifier.attack(ModifierSource.EFFECT, ctx.self().side(), -1,
					ModifierDuration.rounds(1)));
			parts.add(enemy.name() + " " + MatchLog.signed(-2) + " ATK in the first round and " + MatchLog.signed(-1)
					+ " in the second (ATK " + MatchLog.number(enemy.attack()) + ")");
		});
		EffectActions.log(ctx, parts);
	}
}
