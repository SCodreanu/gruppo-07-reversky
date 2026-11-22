package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;

/**
 * Il Pastore Muto (The Silent Shepherd), Uncommon (Theory of the Invitation), <i>Custode del gregge</i>: "quando
 * muore, la prossima creatura del proprietario entra con +2 DEF".
 * <p>
 * Modificatore in attesa (D-34) con la DEF come scudo, come le Spell (deciso il 18 novembre); vale anche se muoiono
 * entrambe.
 */
public final class SilentShepherdEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SILENT_SHEPHERD";

	private static final int NEXT_CREATURE_SHIELD = 2;

	/** Crea l'effetto. */
	public SilentShepherdEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onDeath(CreatureContext ctx) {
		CreatureInPlay self = ctx.self();
		ctx.owner().addPendingModifier(new PendingModifier(self.name(), ModifierSource.EFFECT, self.side(), 0,
				NEXT_CREATURE_SHIELD, 0));
		EffectActions.log(ctx, List.of(MatchLog.signed(NEXT_CREATURE_SHIELD) + " DEF to the next creature"));
	}
}
