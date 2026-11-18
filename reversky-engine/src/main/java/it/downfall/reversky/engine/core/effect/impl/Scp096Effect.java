package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.StatModifier;

/**
 * SCP-096 (Shy Guy), Uncommon (Theory of the Archive), <i>Il volto visto</i>: "la prima creatura avversaria che lo
 * ferisce viene marchiata; +3 ATK contro quella creatura".
 * <p>
 * Ferire vuol dire togliergli DEF davvero con un attacco, dopo gli scudi (docs/effetti, "Soglie"). Si marchia una sola
 * creatura per entrata in campo (D-31); il bonus dura finché le due si affrontano, cioè fino a fine scontro, perché la
 * creatura marchiata non torna in campo.
 */
public final class Scp096Effect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SCP_096";

	private static final int ATTACK_BONUS = 3;

	private CreatureInPlay marked;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void afterDamage(CreatureContext ctx, int damage, int wound) {
		CreatureInPlay self = ctx.self();
		CreatureInPlay attacker = ctx.enemy().orElse(null);
		if (marked != null || wound <= 0 || attacker == null || self.isDead()) {
			return;
		}
		marked = attacker;
		self.addModifier(StatModifier.attack(ModifierSource.EFFECT, self.side(), ATTACK_BONUS,
				ModifierDuration.untilCombatEnd()));
		ctx.log().effectTriggered(self.side(), self.name(), "marks " + attacker.name() + ": "
				+ MatchLog.signed(ATTACK_BONUS) + " ATK against it (ATK " + MatchLog.number(self.attack()) + ")");
	}
}
