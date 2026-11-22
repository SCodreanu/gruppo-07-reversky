package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.HiddenCondition;
import it.downfall.reversky.engine.core.effect.HiddenEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.Penalty;
import it.downfall.reversky.engine.core.model.StatModifier;

/**
 * Remnants of Reality, Hidden {@code SURVIVAL} con condizione: "quando la propria creatura riceve una penalità,
 * annulla fino a 5 punti di penalità (prima quella che l'ha attivata, poi quelle già presenti, in ordine), senza
 * superare i valori di prima; i punti avanzati si perdono".
 * <p>
 * Si attiva al controllo dopo la fase in cui arriva la penalità (ENG-37, deciso il 18 novembre). Penalità = DEF persa
 * per una carta avversaria (effetto, Spell, Hidden, danno in attesa, Devouring Abyss) o malus ATK di una carta
 * avversaria; sigilli e congelamenti no. Prima ridà la DEF tolta dalle penalità di questa fase, nell'ordine, anche a
 * una creatura con DEF 0 o meno (così le evita la morte) e mai più di quanta ne hanno tolta; poi toglie punti ai malus
 * ATK avversari ancora attivi, dal più vecchio. La DEF persa nelle fasi prima resta persa.
 */
public final class RemnantsOfRealityEffect implements HiddenEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "REMNANTS_OF_REALITY";

	private static final int POINTS = 5;

	/** Crea l'effetto. */
	public RemnantsOfRealityEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public HiddenCondition condition() {
		return HiddenCondition.PENALTY;
	}

	@Override
	public void cancelPenalties(EffectContext ctx, CreatureInPlay creature, List<Penalty> penalties) {
		List<Penalty> order = new ArrayList<>(penalties);
		for (StatModifier malus : creature.enemyAttackPenalties()) {
			if (penalties.stream().noneMatch(penalty -> penalty.attackMalus() == malus)) {
				order.add(new Penalty(0, malus));
			}
		}
		int left = POINTS;
		for (Penalty penalty : order) {
			if (left == 0) {
				break;
			}
			int cancelled;
			if (penalty.isAttack()) {
				cancelled = creature.cancelAttackPenalty(penalty.attackMalus(), left);
			} else {
				cancelled = Math.min(left, penalty.defense());
				creature.restoreDefense(cancelled);
			}
			left -= cancelled;
		}
		ctx.log().hiddenTriggered(ctx.owner().side(), ctx.card(), "cancels " + (POINTS - left)
				+ " penalty points on " + creature.name() + " (ATK " + MatchLog.number(creature.attack()) + ", DEF "
				+ MatchLog.number(creature.defense()) + ")");
	}
}
