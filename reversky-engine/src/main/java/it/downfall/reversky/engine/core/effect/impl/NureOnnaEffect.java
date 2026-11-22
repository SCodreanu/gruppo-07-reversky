package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.CreatureInPlay;

/**
 * Nure-onna, Rare (Theory of the Dream), <i>Ogni vittima</i>: "una volta per ogni creatura avversaria, quando la sua
 * DEF scende a metà o meno, 3 danni".
 * <p>
 * Soglia M-06: DEF ≤ 50% della DEF all'entrata dell'avversaria, con l'avversaria ancora viva, controllata dopo ogni
 * fase ({@code onDefenseCheck}), quindi anche dopo un danno da effetto. Una sopravvissuta già a metà o meno è colpita
 * al primo controllo (deciso il 18 novembre). Stato: l'ultima avversaria colpita. Se il Fascicolo Sigillato 0 annulla
 * l'attivazione, quell'avversaria è comunque consumata (deciso il 18 novembre).
 */
public final class NureOnnaEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "NURE_ONNA";

	private static final int DAMAGE = 3;

	/** Ultima avversaria contro cui l'effetto si è attivato. */
	private CreatureInPlay lastVictim;

	/** Crea l'effetto. */
	public NureOnnaEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onDefenseCheck(CreatureContext ctx) {
		CreatureInPlay enemy = ctx.enemy().filter(creature -> !creature.isDead()).orElse(null);
		if (enemy == null || enemy == lastVictim || enemy.defense() * 2 > enemy.initialDefense()) {
			return;
		}
		lastVictim = enemy;
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(target -> parts.add(EffectActions.damage(ctx, target, DAMAGE)));
		EffectActions.log(ctx, parts);
	}
}
