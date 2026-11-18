package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;

/**
 * The Threadbound Doll, Uncommon (Theory of the Dream): "quando muore, 4 danni all'avv.".
 * <p>
 * Danno da effetto (D-08), come quello di SCP-2317: se uccide la sopravvissuta muoiono entrambe e nessun giocatore
 * perde punti vita (docs/effetti, "Effetto alla morte").
 */
public final class ThreadboundDollEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "THREADBOUND_DOLL";

	private static final int DAMAGE = 4;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onDeath(CreatureContext ctx) {
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> parts.add(EffectActions.damage(ctx, enemy, DAMAGE)));
		EffectActions.log(ctx, parts);
	}
}
