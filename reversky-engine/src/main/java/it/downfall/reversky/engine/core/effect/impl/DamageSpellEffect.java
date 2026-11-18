package it.downfall.reversky.engine.core.effect.impl;

import java.util.Optional;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.SpellEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;

/**
 * Base delle Spell {@code DAMAGE} (docs/effetti/catalogo-effetti, "Spell"): un malus ATK e un danno alla DEF della
 * creatura avversaria.
 * <p>
 * Colpiscono <b>subito</b> la creatura avversaria in campo (D-10); se l'avversario non ne ha, il colpo resta in attesa
 * della sua prossima creatura (D-11, D-34). Il malus ATK dura finché la creatura colpita resta in campo (D-12); il
 * danno è un danno da effetto, una perdita permanente di DEF (D-49) che può uccidere (D-08).
 */
public abstract class DamageSpellEffect implements SpellEffect {

	private final int attackPenalty;
	private final int damage;

	/**
	 * Crea la base.
	 *
	 * @param attackPenalty punti di ATK tolti, almeno 0
	 * @param damage        danno da effetto alla DEF, almeno 0
	 */
	protected DamageSpellEffect(int attackPenalty, int damage) {
		this.attackPenalty = attackPenalty;
		this.damage = damage;
	}

	@Override
	public final void resolve(EffectContext ctx) {
		PendingModifier hit = new PendingModifier(ctx.card().name(), ModifierSource.SPELL, ctx.owner().side(),
				-attackPenalty, 0, damage);
		Optional<CreatureInPlay> target = ctx.opponent().creature();
		if (target.isPresent()) {
			hit.applyTo(target.get());
		} else {
			ctx.opponent().addPendingModifier(hit);
		}
		ctx.log().spellResolved(ctx.owner().side(), ctx.card(), MatchLog.describe(hit),
				target.map(CreatureInPlay::name).orElse(null));
	}
}
