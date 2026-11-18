package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;

/**
 * Base degli effetti "all'entrata avv. −N DEF e −N DEF alla prossima creatura avversaria che entra" (classi base,
 * D-37): Cervo Bianco, Il Collezionista di Nomi e poi Cartellino Sbiadito, Rokurokubi.
 * <p>
 * Il primo colpo è danno da effetto (D-08); il secondo è un modificatore in attesa nel {@code PlayerState} avversario
 * (D-34), che il Fascicolo Sigillato 0 controlla quando arriva.
 */
public abstract class EntryAndNextDamageEffect implements CreatureEffect {

	private final int damage;

	/**
	 * Crea la base.
	 *
	 * @param damage danno all'avversaria in campo e alla prossima, almeno 1
	 */
	protected EntryAndNextDamageEffect(int damage) {
		this.damage = damage;
	}

	@Override
	public final void onEnterPlay(CreatureContext ctx) {
		CreatureInPlay self = ctx.self();
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> parts.add(EffectActions.damage(enemy, damage)));
		ctx.opponent().addPendingModifier(new PendingModifier(self.name(), ModifierSource.EFFECT, self.side(), 0, 0,
				damage));
		parts.add(MatchLog.signed(-damage) + " DEF to the next enemy creature");
		EffectActions.log(ctx, parts);
	}
}
