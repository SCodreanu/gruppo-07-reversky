package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;

/**
 * Jorōgumo, Rare (Theory of the Dream), <i>Doppia illusione</i>: "previene 3 danni da ciascuno dei primi due attacchi
 * subiti".
 * <p>
 * Contano gli attacchi con danno sopra 0 dopo l'entrata in campo, di qualunque avversaria (D-31, D-33); un attacco
 * da 0 non consuma la prevenzione. Il danno da effetto non è un attacco e non si riduce.
 */
public final class JorogumoEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "JOROGUMO";

	private static final int PREVENTED = 3;
	private static final int ATTACKS = 2;

	private int attacksPrevented;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public int modifyIncomingDamage(CreatureContext ctx, int damage) {
		if (damage <= 0 || attacksPrevented >= ATTACKS) {
			return damage;
		}
		attacksPrevented++;
		int reduced = Math.max(0, damage - PREVENTED);
		EffectActions.log(ctx, List.of("prevents " + (damage - reduced) + " damage"));
		return reduced;
	}
}
