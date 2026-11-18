package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * SCP-053 (The Young Girl), Uncommon (Theory of the Archive), <i>Paranoia</i>: "all'entrata avv. −3 DEF; l'avv. ha +2
 * ATK nel primo round".
 * <p>
 * Il −3 è danno da effetto (D-08). Tutto quello che tocca l'avversaria passa da un solo controllo del Fascicolo
 * Sigillato 0.
 */
public final class Scp053Effect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SCP_053";

	private static final int DAMAGE = 3;
	private static final int ENEMY_ATTACK_BONUS = 2;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> {
			parts.add(EffectActions.damage(enemy, DAMAGE));
			parts.add(EffectActions.attack(enemy, ctx.self(), ENEMY_ATTACK_BONUS, ModifierDuration.rounds(1),
					"in the first round"));
		});
		EffectActions.log(ctx, parts);
	}
}
