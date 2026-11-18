package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;

/**
 * SCP-2317 (A Door in the Woods), Mythic (Theory of the Archive): "quando muore, 4 danni all'avv. e +1 ATK alla
 * prossima propria creatura".
 * <p>
 * Il danno è danno da effetto (D-08) e può uccidere anche l'avversaria: allora muoiono entrambe e nessun giocatore
 * perde punti vita (docs/effetti, "Effetto alla morte"). Un Fascicolo Sigillato 0 dell'avversario annulla il danno, non
 * il bonus. Il +1 ATK è un modificatore in attesa (D-34) e dura finché la prossima creatura resta in campo.
 */
public final class Scp2317Effect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SCP_2317";

	private static final int DAMAGE = 4;
	private static final int NEXT_CREATURE_ATTACK = 1;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onDeath(CreatureContext ctx) {
		CreatureInPlay self = ctx.self();
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> {
			enemy.takeDamage(DAMAGE);
			parts.add("deals " + DAMAGE + " damage to " + enemy.name() + " (DEF " + MatchLog.number(enemy.defense())
					+ ")");
		});
		ctx.owner().addPendingModifier(new PendingModifier(self.name(), ModifierSource.EFFECT, self.side(),
				NEXT_CREATURE_ATTACK, 0, 0));
		parts.add(MatchLog.signed(NEXT_CREATURE_ATTACK) + " ATK to the next creature");
		ctx.log().effectTriggered(self.side(), self.name(), String.join(", ", parts));
	}
}
