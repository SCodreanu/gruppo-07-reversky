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
 * Smiler, Legendary (Theory of the Wound), <i>Sorriso nel buio</i>: "per ogni round in cui sopravvive, +2 ATK
 * (massimo +6) e l'avv. −2 ATK (massimo −4)".
 * <p>
 * Un round conta anche se l'avversaria muore in quel round (deciso il 18 novembre); il malus allora non serve. Il bonus
 * resta contro le nuove avversarie; il malus resta sulla creatura che l'ha ricevuto e riparte da 0 contro una nuova
 * avversaria (docs/effetti/catalogo-effetti).
 */
public final class SmilerEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SMILER";

	private static final int ATTACK_STEP = 2;
	private static final int MAX_BONUS = 6;
	private static final int MAX_PENALTY = 4;

	private int bonusGained;
	private CreatureInPlay penalized;
	private int penaltyGiven;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onRoundEnd(CreatureContext ctx) {
		CreatureInPlay self = ctx.self();
		List<String> parts = new ArrayList<>();
		if (bonusGained < MAX_BONUS) {
			bonusGained += ATTACK_STEP;
			self.addModifier(StatModifier.attack(ModifierSource.EFFECT, self.side(), ATTACK_STEP,
					ModifierDuration.whileInPlay()));
			parts.add(MatchLog.signed(ATTACK_STEP) + " ATK (ATK " + MatchLog.number(self.attack()) + ")");
		}
		CreatureInPlay enemy = ctx.enemy().orElse(null);
		if (enemy != penalized) {
			penalized = enemy;
			penaltyGiven = 0;
		}
		if (penaltyGiven < MAX_PENALTY) {
			ctx.targetEnemy().ifPresent(target -> {
				penaltyGiven += ATTACK_STEP;
				target.addModifier(StatModifier.attack(ModifierSource.EFFECT, self.side(), -ATTACK_STEP,
						ModifierDuration.whileInPlay()));
				parts.add(target.name() + " " + MatchLog.signed(-ATTACK_STEP) + " ATK (ATK "
						+ MatchLog.number(target.attack()) + ")");
			});
		}
		if (!parts.isEmpty()) {
			ctx.log().effectTriggered(self.side(), self.name(), String.join(", ", parts));
		}
	}
}
