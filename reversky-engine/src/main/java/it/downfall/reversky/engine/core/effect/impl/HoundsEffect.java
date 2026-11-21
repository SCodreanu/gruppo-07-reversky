package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.CreatureInPlay;

/**
 * Hounds, Mythic (Theory of the Wound), <i>Odore del sangue</i>: "+4 ATK contro ogni creatura avversaria già ferita
 * (DEF più bassa della sua DEF all'entrata)".
 * <p>
 * Si controlla a ogni attacco sull'avversaria in campo, senza stato: dipende dall'avversaria del momento, quindi sta
 * in {@code modifyOutgoingAttack} e compare come {@code effectBonus}. Conta la DEF attuale sotto quella all'entrata
 * (deciso il 18 novembre): anche uno scudo consumato o il bonus di un Terreno uscito.
 */
public final class HoundsEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "HOUNDS";

	private static final int ATTACK_BONUS = 4;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public int modifyOutgoingAttack(CreatureContext ctx, int attack) {
		CreatureInPlay enemy = ctx.enemy().orElse(null);
		if (enemy == null || enemy.defense() >= enemy.initialDefense()) {
			return attack;
		}
		EffectActions.log(ctx, List.of("smells blood on " + enemy.name() + ": +" + ATTACK_BONUS + " ATK"));
		return attack + ATTACK_BONUS;
	}
}
