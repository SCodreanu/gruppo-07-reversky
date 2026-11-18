package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;

/**
 * The Stitched Corridor, Mutant (Theory of the Wound + Theory of the Archive), <i>Battito per ogni vittima</i>:
 * "contro ogni creatura avversaria, 2 danni quando entra (o quando entra il Corridor) e il suo primo attacco fa 3 danni
 * in meno".
 * <p>
 * I 2 danni arrivano a ogni inizio di scontro ({@code onCombatStart}, come Mothman) e sono danno da effetto (D-08); la
 * prevenzione è quella del gruppo di Hidebehind, contro ogni avversaria (D-33).
 */
public final class StitchedCorridorEffect extends FirstAttackShieldEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "STITCHED_CORRIDOR";

	private static final int PREVENTED = 3;
	private static final int DAMAGE = 2;

	/** Crea l'effetto. */
	public StitchedCorridorEffect() {
		super(PREVENTED);
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onCombatStart(CreatureContext ctx) {
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> parts.add(EffectActions.damage(ctx, enemy, DAMAGE)));
		EffectActions.log(ctx, parts);
	}
}
