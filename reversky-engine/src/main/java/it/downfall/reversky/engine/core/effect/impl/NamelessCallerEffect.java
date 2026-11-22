package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;

/**
 * The Nameless Caller, Uncommon (Theory of the Invitation), <i>Voci dei dispersi</i>: "all'entrata 1 danno all'avv.
 * per ogni creatura avversaria già morta nella partita, massimo 3".
 * <p>
 * Le creature avversarie morte sono quelle che il proprietario ha sconfitto (D-42 conta ogni morte per chi la stava
 * affrontando, qualunque sia la causa).
 */
public final class NamelessCallerEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "NAMELESS_CALLER";

	private static final int MAX_DAMAGE = 3;

	/** Crea l'effetto. */
	public NamelessCallerEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		int damage = Math.min(MAX_DAMAGE, ctx.owner().creaturesDefeated());
		if (damage == 0) {
			return;
		}
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> parts.add(EffectActions.damage(ctx, enemy, damage)));
		EffectActions.log(ctx, parts);
	}
}
