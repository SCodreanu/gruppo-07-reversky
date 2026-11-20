package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.CreatureInPlay;

/**
 * Il Custode delle Porte Incrociate (The Crossed Doors Keeper), Mythic (Theory of the Wound), <i>Inviolabile</i>: "gli
 * effetti delle creature avversarie non lo colpiscono (niente danni da effetto, malus, annullamenti; gli attacchi sì);
 * +2 ATK contro creature che hanno un effetto".
 * <p>
 * Filtro sul bersaglio, non un effetto {@code NULLIFY} (ENG-38, deciso il 19 novembre): gli effetti avversari non lo
 * trovano in {@link CreatureContext#targetEnemy()} (prima del Fascicolo Sigillato 0, che non si consuma) e il motore
 * scarta i modificatori in attesa degli effetti avversari che arrivano a lui. Gli effetti avversari che lo leggono
 * soltanto (Hounds, Bigfoot, Skin-Stealers) funzionano; Spell, Hidden, Terreni e Oggetti avversari lo colpiscono
 * normalmente. Il +2 ATK vale se la carta avversaria ha un effetto, anche annullato; sta in
 * {@code modifyOutgoingAttack}, quindi nessun sigillo lo tocca.
 */
public final class CrossedDoorsKeeperEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "CROSSED_DOORS_KEEPER";

	private static final int ATTACK_BONUS = 2;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public boolean immuneToEnemyCreatureEffects() {
		return true;
	}

	@Override
	public int modifyOutgoingAttack(CreatureContext ctx, int attack) {
		CreatureInPlay enemy = ctx.enemy().orElse(null);
		if (enemy == null || !EffectActions.hasEffect(enemy)) {
			return attack;
		}
		EffectActions.log(ctx, List.of("+" + ATTACK_BONUS + " ATK against " + enemy.name()));
		return attack + ATTACK_BONUS;
	}
}
