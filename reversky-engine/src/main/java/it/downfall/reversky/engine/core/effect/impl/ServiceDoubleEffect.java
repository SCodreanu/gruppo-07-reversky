package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * Il Doppio di Servizio (The Service Double), Rare (Theory of the Wound), <i>Doppio turno</i>: "+3 ATK nei primi due
 * round dopo l'entrata".
 * <p>
 * Come Kappa: un bonus ATK di due round dato all'entrata (D-31), un modificatore con origine {@code EFFECT} (deciso il
 * 18 novembre).
 */
public final class ServiceDoubleEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SERVICE_DOUBLE";

	private static final int ATTACK_BONUS = 3;
	private static final int ROUNDS = 2;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		EffectActions.log(ctx, List.of(EffectActions.attack(ctx.self(), ctx.self(), ATTACK_BONUS,
				ModifierDuration.rounds(ROUNDS), "in its first two rounds")));
	}
}
