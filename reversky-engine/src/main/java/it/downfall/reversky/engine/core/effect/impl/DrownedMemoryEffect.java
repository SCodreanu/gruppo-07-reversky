package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * The Drowned Memory, Rare (Theory of the Archive), <i>Memoria profonda</i>: "all'entrata +1 DEF per ogni creatura
 * alleata già eliminata (massimo +4) e +1 ATK se sono almeno 3, finché resta in campo".
 * <p>
 * Le creature alleate eliminate sono quelle che l'avversario ha sconfitto (D-42). La DEF è permanente, come per Il
 * Raccoglitore di Orme.
 */
public final class DrownedMemoryEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "DROWNED_MEMORY";

	private static final int MAX_DEFENSE = 4;
	private static final int ATTACK_THRESHOLD = 3;
	private static final int ATTACK_BONUS = 1;

	/** Crea l'effetto. */
	public DrownedMemoryEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		int lost = ctx.opponent().creaturesDefeated();
		List<String> parts = new ArrayList<>();
		if (lost > 0) {
			parts.add(EffectActions.defense(ctx.self(), Math.min(MAX_DEFENSE, lost)));
		}
		if (lost >= ATTACK_THRESHOLD) {
			parts.add(EffectActions.attack(ctx.self(), ctx.self(), ATTACK_BONUS, ModifierDuration.whileInPlay(),
					"while in play"));
		}
		EffectActions.log(ctx, parts);
	}
}
