package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.CreatureInPlay;

/**
 * The Bloodless Hound, Rare (Theory of the Invitation), <i>Drenaggio senza sangue</i>: "dopo ogni suo attacco che
 * infligge danni recupera metà dei danni inflitti (per difetto), senza superare la DEF all'entrata; dopo
 * un'eliminazione recupera altri 4".
 * <p>
 * I danni inflitti sono quelli del suo attacco dopo il passo 3, scudi e DEF sotto 0 compresi (deciso il 18 novembre,
 * {@code afterAttack}). L'eliminazione è ogni morte dell'avversaria, per qualunque causa ({@code onKill}, D-42). Con
 * DEF 0 o meno non recupera niente.
 */
public final class BloodlessHoundEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "BLOODLESS_HOUND";

	private static final int KILL_HEAL = 4;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void afterAttack(CreatureContext ctx, int damage) {
		EffectActions.heal(ctx.self(), damage / 2, "").ifPresent(part -> EffectActions.log(ctx, List.of(part)));
	}

	@Override
	public void onKill(CreatureContext ctx, CreatureInPlay defeated) {
		EffectActions.heal(ctx.self(), KILL_HEAL, " after defeating " + defeated.name())
				.ifPresent(part -> EffectActions.log(ctx, List.of(part)));
	}
}
