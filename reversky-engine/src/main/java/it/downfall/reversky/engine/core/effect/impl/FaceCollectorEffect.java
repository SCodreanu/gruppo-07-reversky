package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.StatModifier;

/**
 * The Face Collector, Rare (Theory of the Dream), <i>Forza presa in prestito</i>: "ruba 2 ATK nel primo round e 1 ATK
 * nel secondo (l'avv. ha il malus, The Face Collector lo stesso bonus)".
 * <p>
 * Il furto si calcola all'entrata (deciso il 18 novembre) sull'ATK dell'avversaria con i modificatori, senza il bonus
 * di fazione: se ha meno ATK di quanto dovrebbe perdere, ruba solo quello che c'è e guadagna lo stesso. Come per il
 * gruppo di Howlers, due modificatori per parte: uno di due round e uno di un round. Se il Fascicolo Sigillato 0
 * annulla l'attivazione non ruba e non guadagna niente (deciso il 18 novembre).
 */
public final class FaceCollectorEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "FACE_COLLECTOR";

	private static final int FIRST_ROUND = 2;
	private static final int SECOND_ROUND = 1;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		CreatureInPlay self = ctx.self();
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> {
			int available = Math.max(0, enemy.attack());
			int first = Math.min(FIRST_ROUND, available);
			int second = Math.min(SECOND_ROUND, available);
			if (first == 0) {
				parts.add("has no ATK to steal from " + enemy.name());
				return;
			}
			// "second" vale due round, la differenza solo il primo
			steal(self, enemy, second, 2);
			steal(self, enemy, first - second, 1);
			parts.add("steals " + first + " ATK in the first round and " + second + " in the second from "
					+ enemy.name() + " (ATK " + MatchLog.number(self.attack()) + ", " + enemy.name() + " ATK "
					+ MatchLog.number(enemy.attack()) + ")");
		});
		EffectActions.log(ctx, parts);
	}

	private static void steal(CreatureInPlay self, CreatureInPlay enemy, int amount, int rounds) {
		if (amount <= 0) {
			return;
		}
		enemy.addModifier(StatModifier.attack(ModifierSource.EFFECT, self.side(), -amount,
				ModifierDuration.rounds(rounds)));
		self.addModifier(StatModifier.attack(ModifierSource.EFFECT, self.side(), amount,
				ModifierDuration.rounds(rounds)));
	}
}
