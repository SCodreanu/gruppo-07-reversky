package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;

/**
 * The Thorned Matriarch, Mythic (Theory of the Invitation), <i>Offerta di rovi</i>: "all'inizio di ogni round, se la
 * sua DEF è più alta di quella avversaria, sacrifica 2 DEF e infligge 4 danni all'avv.".
 * <p>
 * Il sacrificio è una perdita di DEF come un danno da effetto (prima gli scudi) e può ucciderla: allora, se muore
 * anche l'avversaria, nessuno perde punti vita (deciso il 18 novembre). Un Fascicolo Sigillato 0 annulla i 4 danni,
 * non il sacrificio.
 */
public final class ThornedMatriarchEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "THORNED_MATRIARCH";

	private static final int SACRIFICE = 2;
	private static final int DAMAGE = 4;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onRoundStart(CreatureContext ctx) {
		CreatureInPlay self = ctx.self();
		CreatureInPlay enemy = ctx.enemy().orElse(null);
		if (enemy == null || self.defense() <= enemy.defense()) {
			return;
		}
		self.takeDamage(SACRIFICE, self);
		List<String> parts = new ArrayList<>();
		parts.add("sacrifices " + SACRIFICE + " DEF (DEF " + MatchLog.number(self.defense()) + ")");
		ctx.targetEnemy().ifPresent(target -> parts.add(EffectActions.damage(ctx, target, DAMAGE)));
		EffectActions.log(ctx, parts);
	}
}
