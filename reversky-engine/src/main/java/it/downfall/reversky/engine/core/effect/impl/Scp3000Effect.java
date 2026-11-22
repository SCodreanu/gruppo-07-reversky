package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * SCP-3000 (Anantashesha), Mythic (Theory of the Archive), <i>Pelle abissale</i>: "+2 ATK finché resta in campo;
 * contro Rare o superiori subisce metà dei danni dai loro attacchi (per difetto)".
 * <p>
 * Il +2 ATK è un bonus dato all'entrata. La metà si calcola al passo 3, dopo le altre riduzioni; il danno da effetto
 * non si dimezza.
 */
public final class Scp3000Effect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SCP_3000";

	/**
	 * {@code rarityOrder} della rarità Rare ({@code rarities.sorting_order}: Common 1 … Mutant 6). Deve restare uguale
	 * al seed delle rarità dell'API (API-15): deciso il 18 novembre.
	 */
	private static final int RARE_ORDER = 3;
	private static final int ATTACK_BONUS = 2;

	/** Crea l'effetto. */
	public Scp3000Effect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		EffectActions.log(ctx, List.of(EffectActions.attack(ctx.self(), ctx.self(), ATTACK_BONUS,
				ModifierDuration.whileInPlay(), "while in play")));
	}

	@Override
	public int modifyIncomingDamage(CreatureContext ctx, int damage) {
		CreatureInPlay attacker = ctx.enemy().orElse(null);
		if (damage <= 0 || attacker == null || attacker.card().rarityOrder() < RARE_ORDER) {
			return damage;
		}
		int halved = damage / 2;
		EffectActions.log(ctx, List.of("halves the damage from " + attacker.name() + " (" + damage + " → " + halved
				+ ")"));
		return halved;
	}
}
