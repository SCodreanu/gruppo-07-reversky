package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * Deathmoths, Mythic (Theory of the Wound), <i>Divoratrici di luce</i>: "all'entrata avv. −2 ATK finché resta in campo
 * e perde fino a 2 punti dei suoi bonus ATK, anche quelli ricevuti dopo".
 * <p>
 * Malus e sigillo (ENG-35, deciso il 18 novembre) restano sull'avversaria per tutta la sua permanenza in campo, anche
 * dopo la morte di Deathmoths. Il sigillo non tocca i malus, quindi neanche il suo −2.
 */
public final class DeathmothsEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "DEATHMOTHS";

	private static final int PENALTY = -2;
	private static final int SEAL = 2;

	/** Crea l'effetto. */
	public DeathmothsEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> {
			parts.add(EffectActions.seal(enemy, ctx.self(), SEAL, ModifierDuration.whileInPlay()));
			parts.add(EffectActions.attack(enemy, ctx.self(), PENALTY, ModifierDuration.whileInPlay(),
					"while in play"));
		});
		EffectActions.log(ctx, parts);
	}
}
