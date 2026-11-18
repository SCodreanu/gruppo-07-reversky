package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.HiddenEffect;
import it.downfall.reversky.engine.core.effect.Penalties;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.StatModifier;

/**
 * Base delle Hidden {@code DAMAGE} "prima del calcolo dello scontro" (docs/effetti/catalogo-effetti, "Hidden"): un
 * malus ATK alla creatura avversaria che vale <b>solo il primo round</b> dello scontro, e un danno alla DEF, che è
 * permanente (danno da effetto, D-49).
 */
public abstract class FirstRoundHiddenEffect implements HiddenEffect {

	private static final int FIRST_ROUND = 1;

	private final int attackPenalty;
	private final int damage;

	/**
	 * Crea la base.
	 *
	 * @param attackPenalty punti di ATK tolti nel primo round, almeno 0
	 * @param damage        danno da effetto alla DEF, almeno 0
	 */
	protected FirstRoundHiddenEffect(int attackPenalty, int damage) {
		this.attackPenalty = attackPenalty;
		this.damage = damage;
	}

	@Override
	public final void trigger(EffectContext ctx) {
		CreatureInPlay target = ctx.opponent().creature().orElseThrow(
				() -> new IllegalStateException("Hidden DAMAGE senza creatura avversaria"));
		List<String> parts = new ArrayList<>();
		if (attackPenalty > 0) {
			target.addModifier(StatModifier.attack(ModifierSource.HIDDEN, ctx.owner().side(), -attackPenalty,
					ModifierDuration.rounds(FIRST_ROUND)));
			parts.add(MatchLog.signed(-attackPenalty) + " ATK in the first round");
		}
		if (damage > 0) {
			int dealt = Penalties.defense(ctx.state(), target, damage, ctx.owner().side(), null);
			parts.add(MatchLog.signed(-dealt) + " DEF");
		}
		ctx.log().hiddenTriggered(ctx.owner().side(), ctx.card(),
				String.join(", ", parts) + " to " + target.name());
	}
}
