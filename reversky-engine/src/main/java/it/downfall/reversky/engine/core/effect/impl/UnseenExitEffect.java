package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.HiddenCondition;
import it.downfall.reversky.engine.core.effect.HiddenEffect;
import it.downfall.reversky.engine.core.model.CreatureInPlay;

/**
 * Unseen Exit, Hidden {@code SURVIVAL} con condizione: "quando un attacco avversario ucciderebbe la propria creatura,
 * previene 5 danni (controllo al passo 3, dopo tutte le altre riduzioni)".
 * <p>
 * "Ucciderebbe" = il danno dell'attacco arriva almeno alla DEF attuale, scudi compresi. Si attiva e si scarta anche se
 * 5 danni in meno non bastano a salvarla (ENG-37, deciso il 18 novembre). Un danno da effetto non la attiva.
 */
public final class UnseenExitEffect implements HiddenEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "UNSEEN_EXIT";

	private static final int PREVENTED = 5;

	/** Crea l'effetto. */
	public UnseenExitEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public HiddenCondition condition() {
		return HiddenCondition.LETHAL_ATTACK;
	}

	@Override
	public int preventLethalAttack(EffectContext ctx, CreatureInPlay creature, int damage) {
		ctx.log().hiddenTriggered(ctx.owner().side(), ctx.card(),
				"prevents " + PREVENTED + " damage to " + creature.name());
		return Math.max(0, damage - PREVENTED);
	}
}
