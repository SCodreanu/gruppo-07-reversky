package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.HiddenEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.StatModifier;

/**
 * Base delle Hidden {@code SURVIVAL} senza condizione (docs/effetti/catalogo-effetti, "Hidden"): ATK e DEF in più alla
 * propria creatura "nello scontro corrente", cioè finché una delle due creature muore.
 * <p>
 * La DEF in più è uno scudo (D-35): assorbe i danni per primo e alla fine dello scontro sparisce solo la parte non
 * usata, quindi la scadenza non uccide.
 */
public abstract class SurvivalHiddenEffect implements HiddenEffect {

	private final int attackBonus;
	private final int defenseBonus;

	/**
	 * Crea la base.
	 *
	 * @param attackBonus  ATK in più, almeno 0
	 * @param defenseBonus DEF in più, almeno 0
	 */
	protected SurvivalHiddenEffect(int attackBonus, int defenseBonus) {
		this.attackBonus = attackBonus;
		this.defenseBonus = defenseBonus;
	}

	@Override
	public final void trigger(EffectContext ctx) {
		CreatureInPlay creature = ctx.owner().creature().orElseThrow(
				() -> new IllegalStateException("Hidden SURVIVAL senza creatura del proprietario"));
		List<String> parts = new ArrayList<>();
		if (attackBonus > 0) {
			creature.addModifier(StatModifier.attack(ModifierSource.HIDDEN, ctx.owner().side(), attackBonus,
					ModifierDuration.untilCombatEnd()));
			parts.add(MatchLog.signed(attackBonus) + " ATK");
		}
		if (defenseBonus > 0) {
			creature.addModifier(StatModifier.shield(ModifierSource.HIDDEN, ctx.owner().side(), defenseBonus,
					ModifierDuration.untilCombatEnd()));
			parts.add(MatchLog.signed(defenseBonus) + " DEF");
		}
		ctx.log().hiddenTriggered(ctx.owner().side(), ctx.card(),
				String.join(", ", parts) + " to " + creature.name() + " this combat");
	}
}
