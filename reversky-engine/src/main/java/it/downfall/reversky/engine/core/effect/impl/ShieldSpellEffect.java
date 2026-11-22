package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.SpellEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;

/**
 * Base delle Spell {@code SHIELD} con un bonus fisso (docs/effetti/catalogo-effetti, "Spell"): ATK e DEF in più alla
 * prossima creatura del proprietario.
 * <p>
 * Una Spell si pesca solo quando la propria creatura è morta, quindi il bonus resta in attesa e si somma agli altri
 * sulla prossima creatura (D-10, D-34). Dura finché quella creatura resta in campo (D-12); la DEF è uno scudo (D-35).
 */
public abstract class ShieldSpellEffect implements SpellEffect {

	private final int attackBonus;
	private final int defenseBonus;

	/**
	 * Crea la base.
	 *
	 * @param attackBonus  ATK in più, almeno 0
	 * @param defenseBonus DEF in più, almeno 0
	 */
	protected ShieldSpellEffect(int attackBonus, int defenseBonus) {
		this.attackBonus = attackBonus;
		this.defenseBonus = defenseBonus;
	}

	@Override
	public final void resolve(EffectContext ctx) {
		PendingModifier bonus = new PendingModifier(ctx.card().name(), ModifierSource.SPELL, ctx.owner().side(),
				attackBonus, defenseBonus, 0);
		ctx.owner().addPendingModifier(bonus);
		ctx.log().spellResolved(ctx.owner().side(), ctx.card(), MatchLog.describe(bonus), null);
	}
}
