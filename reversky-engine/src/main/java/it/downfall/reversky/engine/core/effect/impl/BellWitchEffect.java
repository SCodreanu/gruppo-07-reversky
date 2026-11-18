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
 * Bell Witch, Rare (Theory of the Invitation), <i>Infestazione che resta</i>: "all'inizio del secondo round 2 danni
 * all'avv., una volta; quando muore, la prossima creatura avversaria entra con −2 DEF".
 * <p>
 * Il secondo round si conta dall'entrata in campo (D-31). Il −2 DEF è un danno in attesa nel {@code PlayerState}
 * avversario (D-34).
 */
public final class BellWitchEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "BELL_WITCH";

	private static final int STRIKE_ROUND = 2;
	private static final int STRIKE_DAMAGE = 2;
	private static final int NEXT_ENEMY_DAMAGE = 2;

	private int roundsInPlay;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onRoundStart(CreatureContext ctx) {
		roundsInPlay++;
		if (roundsInPlay != STRIKE_ROUND) {
			return;
		}
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> parts.add(EffectActions.damage(enemy, STRIKE_DAMAGE)));
		EffectActions.log(ctx, parts);
	}

	@Override
	public void onDeath(CreatureContext ctx) {
		CreatureInPlay self = ctx.self();
		ctx.opponent().addPendingModifier(new PendingModifier(self.name(), ModifierSource.EFFECT, self.side(), 0, 0,
				NEXT_ENEMY_DAMAGE));
		EffectActions.log(ctx, List.of(MatchLog.signed(-NEXT_ENEMY_DAMAGE) + " DEF to the next enemy creature"));
	}
}
