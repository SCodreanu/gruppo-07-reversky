package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * Bigfoot, Mythic (Theory of the Invitation), <i>Ammazzagiganti</i>: "contro Rare o superiori +4 ATK e 2 danni in meno
 * da ogni loro attacco; contro Common e Uncommon +2 ATK".
 * <p>
 * Si controlla contro ogni avversaria, a ogni inizio di scontro: il bonus dura fino a fine scontro. Le rarità si
 * confrontano con {@code rarityOrder} (D-20: Mutant sopra Legendary).
 */
public final class BigfootEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "BIGFOOT";

	/**
	 * {@code rarityOrder} della rarità Rare ({@code rarities.sorting_order}: Common 1 … Mutant 6). Deve restare uguale
	 * al seed delle rarità dell'API (API-15): deciso il 18 novembre.
	 */
	private static final int RARE_ORDER = 3;
	private static final int ATTACK_VS_RARE = 4;
	private static final int ATTACK_VS_COMMON = 2;
	private static final int PREVENTED_VS_RARE = 2;

	/** Crea l'effetto. */
	public BigfootEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onCombatStart(CreatureContext ctx) {
		ctx.enemy().ifPresent(enemy -> {
			int bonus = isRareOrHigher(enemy) ? ATTACK_VS_RARE : ATTACK_VS_COMMON;
			EffectActions.log(ctx, List.of(EffectActions.attack(ctx.self(), ctx.self(), bonus,
					ModifierDuration.untilCombatEnd(), "against " + enemy.name())));
		});
	}

	@Override
	public int modifyIncomingDamage(CreatureContext ctx, int damage) {
		CreatureInPlay attacker = ctx.enemy().orElse(null);
		if (damage <= 0 || attacker == null || !isRareOrHigher(attacker)) {
			return damage;
		}
		int reduced = Math.max(0, damage - PREVENTED_VS_RARE);
		EffectActions.log(ctx, List.of("prevents " + (damage - reduced) + " damage from " + attacker.name()));
		return reduced;
	}

	private static boolean isRareOrHigher(CreatureInPlay creature) {
		return creature.card().rarityOrder() >= RARE_ORDER;
	}
}
