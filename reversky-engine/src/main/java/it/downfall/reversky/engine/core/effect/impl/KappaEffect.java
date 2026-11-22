package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.StatModifier;

/**
 * Kappa, Uncommon (Theory of the Dream): "+3 ATK nel primo round".
 * <p>
 * Il primo round si conta dall'entrata in campo (D-31): un bonus ATK di un round, dato all'entrata.
 */
public final class KappaEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "KAPPA";

	private static final int ATTACK_BONUS = 3;

	/** Crea l'effetto. */
	public KappaEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		CreatureInPlay self = ctx.self();
		self.addModifier(StatModifier.attack(ModifierSource.EFFECT, self.side(), ATTACK_BONUS,
				ModifierDuration.rounds(1)));
		ctx.log().effectTriggered(self.side(), self.name(), MatchLog.signed(ATTACK_BONUS)
				+ " ATK in its first round (ATK " + MatchLog.number(self.attack()) + ")");
	}
}
