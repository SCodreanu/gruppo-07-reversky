package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;

/**
 * Base degli effetti "all'entrata N danni all'avv." (docs/effetti/catalogo-effetti, classi base, D-37): SCP-173 e poi
 * The Gnawing Witness. Il danno è danno da effetto e può uccidere all'entrata (D-08).
 */
public abstract class EntryDamageEffect implements CreatureEffect {

	private final int damage;

	/**
	 * Crea la base.
	 *
	 * @param damage danno all'avversaria, almeno 1
	 */
	protected EntryDamageEffect(int damage) {
		this.damage = damage;
	}

	@Override
	public final void onEnterPlay(CreatureContext ctx) {
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> parts.add(EffectActions.damage(enemy, damage)));
		EffectActions.log(ctx, parts);
	}
}
