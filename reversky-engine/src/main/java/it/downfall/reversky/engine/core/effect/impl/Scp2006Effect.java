package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;

/**
 * SCP-2006 (Too Spooky), Uncommon (Theory of the Archive), <i>Imitazione</i>: "all'entrata, se l'ATK base dell'avv. è
 * più alto del suo, prende quell'ATK base finché resta in campo".
 * <p>
 * Il suo ATK base cambia davvero (deciso il 18 novembre): non è un bonus ATK. A parità non cambia nulla. Legge
 * l'avversaria senza colpirla, quindi il Fascicolo Sigillato 0 non c'entra.
 */
public final class Scp2006Effect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SCP_2006";

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		CreatureInPlay self = ctx.self();
		ctx.enemy().filter(enemy -> enemy.baseAttack() > self.baseAttack()).ifPresent(enemy -> {
			self.copyBaseAttack(enemy.baseAttack());
			EffectActions.log(ctx, List.of("copies the base ATK of " + enemy.name() + " (ATK "
					+ MatchLog.number(self.attack()) + ")"));
		});
	}
}
