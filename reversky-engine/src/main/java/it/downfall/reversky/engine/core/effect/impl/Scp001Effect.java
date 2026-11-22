package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;

/**
 * SCP-001 (When Day Breaks), Legendary (Theory of the Archive), <i>Alba piena</i>: "all'entrata 4 danni, poi 4 danni a
 * inizio round per i 3 round seguenti, alla creatura avversaria in campo in quel momento".
 * <p>
 * I round si contano dall'entrata di SCP-001 (D-31): se l'avversaria muore prima, i colpi restanti vanno alla nuova
 * avversaria. Un colpo annullato dal Fascicolo Sigillato 0 conta lo stesso.
 */
public final class Scp001Effect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SCP_001";

	private static final int DAMAGE = 4;
	private static final int ROUND_STRIKES = 3;

	private int strikesLeft = ROUND_STRIKES;

	/** Crea l'effetto. */
	public Scp001Effect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		strike(ctx);
	}

	@Override
	public void onRoundStart(CreatureContext ctx) {
		if (strikesLeft > 0) {
			strikesLeft--;
			strike(ctx);
		}
	}

	private static void strike(CreatureContext ctx) {
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> parts.add(EffectActions.damage(ctx, enemy, DAMAGE)));
		EffectActions.log(ctx, parts);
	}
}
