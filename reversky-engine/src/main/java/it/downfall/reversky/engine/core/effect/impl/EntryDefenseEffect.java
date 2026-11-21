package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;

/**
 * Base degli effetti "all'entrata +N DEF finché resta in campo" (classi base, D-37): Il Raccoglitore di Orme e The
 * Index Maw. È DEF permanente (catalogo).
 */
public abstract class EntryDefenseEffect implements CreatureEffect {

	private final int defense;

	/**
	 * Crea la base.
	 *
	 * @param defense DEF in più, almeno 1
	 */
	protected EntryDefenseEffect(int defense) {
		this.defense = defense;
	}

	@Override
	public final void onEnterPlay(CreatureContext ctx) {
		EffectActions.log(ctx, List.of(EffectActions.defense(ctx.self(), defense)));
	}
}
