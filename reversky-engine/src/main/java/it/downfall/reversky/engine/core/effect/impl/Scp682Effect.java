package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;

/**
 * SCP-682 (Hard-to-Destroy Reptile), Rare (Theory of the Archive), <i>Adattamento</i>: "ogni attacco subito riduce di
 * 3 i danni degli attacchi successivi, massimo −3 (quindi −3 a ogni attacco dopo il primo)" (erano 2; ENG-42, proposta
 * del 21 novembre).
 * <p>
 * Il primo attacco con danno sopra 0 dall'entrata in campo (D-31, D-33) arriva intero; da quello dopo, di qualunque
 * avversaria, 3 danni in meno.
 */
public final class Scp682Effect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SCP_682";

	private static final int REDUCTION = 3;

	private boolean adapted;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public int modifyIncomingDamage(CreatureContext ctx, int damage) {
		if (damage <= 0) {
			return damage;
		}
		if (!adapted) {
			adapted = true;
			return damage;
		}
		int reduced = Math.max(0, damage - REDUCTION);
		EffectActions.log(ctx, List.of("adapts: prevents " + (damage - reduced) + " damage"));
		return reduced;
	}
}
