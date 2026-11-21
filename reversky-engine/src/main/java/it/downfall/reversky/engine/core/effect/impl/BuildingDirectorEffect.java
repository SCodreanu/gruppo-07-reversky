package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;

/**
 * Il Direttore dell'Edificio (The Building Director), Rare (Theory of the Wound), <i>Demolizione</i>: "all'entrata
 * danni all'avv. pari al 40% della sua DEF base (per difetto, massimo 4)".
 * <p>
 * DEF base = la DEF della carta avversaria. È danno da effetto (D-08) e non cambia la DEF all'entrata dell'avversaria
 * usata dalle soglie (M-06, deciso il 18 novembre).
 */
public final class BuildingDirectorEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "BUILDING_DIRECTOR";

	private static final int PERCENT = 40;
	private static final int MAX_DAMAGE = 4;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> {
			int damage = Math.min(MAX_DAMAGE, enemy.card().defense() * PERCENT / 100);
			if (damage > 0) {
				parts.add(EffectActions.damage(ctx, enemy, damage));
			}
		});
		EffectActions.log(ctx, parts);
	}
}
