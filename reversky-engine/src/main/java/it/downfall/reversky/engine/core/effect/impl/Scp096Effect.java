package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.StatModifier;
import it.downfall.reversky.engine.core.model.Wound;

/**
 * SCP-096 (Shy Guy), Uncommon (Theory of the Archive), <i>Il volto visto</i>: "la prima creatura avversaria che lo
 * ferisce viene marchiata; +3 ATK contro quella creatura".
 * <p>
 * Ferire vuol dire togliergli DEF davvero, dopo gli scudi (docs/effetti, "Soglie"), con un attacco o con un effetto
 * (deciso il 18 novembre): la ferita deve venire da una creatura avversaria, quindi una Spell, una Hidden, un Terreno
 * o un Oggetto non marchiano nessuno. Se lo ferisce all'entrata l'effetto della creatura avversaria, il bonus vale già
 * nel primo round. Si marchia una sola creatura per entrata in campo (D-31); il bonus dura fino a fine scontro, perché
 * la creatura marchiata non torna in campo.
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
	public void onWounded(CreatureContext ctx, Wound wound) {
		CreatureInPlay self = ctx.self();
		CreatureInPlay cause = wound.cause().filter(creature -> creature.side() != self.side()).orElse(null);
		if (marked != null || cause == null) {
			return;
		}
		marked = cause;
		self.addModifier(StatModifier.attack(ModifierSource.EFFECT, self.side(), ATTACK_BONUS,
				ModifierDuration.untilCombatEnd()));
		ctx.log().effectTriggered(self.side(), self.name(), "marks " + cause.name() + ": "
				+ MatchLog.signed(ATTACK_BONUS) + " ATK against it (ATK " + MatchLog.number(self.attack()) + ")");
	}
}
