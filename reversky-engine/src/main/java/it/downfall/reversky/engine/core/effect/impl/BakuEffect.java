package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.effect.EffectPriority;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * Baku, Mythic (Theory of the Dream), <i>Divoratore di incubi</i>: "finché Baku è in campo, ogni volta che l'effetto di
 * una creatura avversaria sta per attivarsi, Baku lo divora (non avviene) e ottiene +2 DEF".
 * <p>
 * Momento "prima dell'attivazione di un effetto avversario" (M-10, ENG-38, deciso il 19 novembre): il motore lo chiede
 * prima di chiamare un qualunque momento dell'effetto avversario (il suo o quello in prestito, non quelli collegati da
 * una Spell). Baku annulla l'effetto fino a fine scontro, cioè finché Baku è in campo: così anche un effetto continuo
 * resta spento per quella creatura. +2 DEF permanente al massimo una volta per creatura avversaria. In pratica Baku
 * divora ogni avversaria con un effetto appena si affrontano. Baku è pronto dopo il suo {@code onEnterPlay}: con due
 * Baku divora quello che risolve per primo nell'ordine D-30. Il Custode delle Porte Incrociate e il Fascicolo
 * Sigillato 0 lo fermano, perché passa da {@link CreatureContext#targetEnemy()}.
 */
public final class BakuEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "BAKU";

	private static final int DEFENSE_GAIN = 2;

	private boolean ready;
	private CreatureInPlay lastFed;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public EffectPriority priority() {
		return EffectPriority.NULLIFY;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		ready = true;
	}

	@Override
	public boolean onEnemyEffectActivating(CreatureContext ctx, CreatureInPlay enemy) {
		if (!ready || ctx.self().isDead()) {
			return false;
		}
		CreatureInPlay target = ctx.targetEnemy().filter(creature -> creature == enemy).orElse(null);
		if (target == null) {
			return false;
		}
		target.cancelEffect(ModifierDuration.untilCombatEnd());
		List<String> parts = new ArrayList<>();
		parts.add("devours " + target.name() + "'s effect");
		if (target != lastFed) {
			lastFed = target;
			parts.add(EffectActions.defense(ctx.self(), DEFENSE_GAIN));
		}
		EffectActions.log(ctx, parts);
		return true;
	}
}
