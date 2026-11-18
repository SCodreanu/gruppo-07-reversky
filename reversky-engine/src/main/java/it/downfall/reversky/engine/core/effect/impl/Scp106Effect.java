package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;

/**
 * SCP-106 (Old Man), Rare (Theory of the Archive), <i>Riflesso più forte</i>: "riflette 3 danni all'attaccante per
 * ciascuno dei primi due attacchi subiti".
 * <p>
 * Conta un attacco con danno sopra 0 (D-33), per entrata in campo (D-31). Riflette anche se l'attacco lo uccide
 * (deciso il 18 novembre): il danno riflesso è danno da effetto e può uccidere l'attaccante (D-08).
 */
public final class Scp106Effect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SCP_106";

	private static final int REFLECTED = 3;
	private static final int MAX_ATTACKS = 2;

	private int attacksReflected;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void afterDamage(CreatureContext ctx, int damage, int wound) {
		if (damage <= 0 || attacksReflected >= MAX_ATTACKS) {
			return;
		}
		attacksReflected++;
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(attacker -> parts.add("reflects: " + EffectActions.damage(attacker, REFLECTED)));
		EffectActions.log(ctx, parts);
	}
}
