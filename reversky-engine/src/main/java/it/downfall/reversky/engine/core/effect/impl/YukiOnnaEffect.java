package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.StatModifier;

/**
 * Yuki-onna, Rare (Theory of the Dream), <i>Congelata</i>: "all'entrata l'avv. non attacca nel primo round (il suo
 * attacco è 0)".
 * <p>
 * Un modificatore "attacco congelato" sull'avversaria per un round (deciso il 18 novembre): nel calcolo del round il
 * suo attacco vale 0 dopo tutti gli altri modificatori e il suo effetto. È un malus, quindi il Fascicolo Sigillato 0
 * lo annulla. Gli effetti dell'avversaria che non sono attacchi restano.
 */
public final class YukiOnnaEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "YUKI_ONNA";

	/** Crea l'effetto. */
	public YukiOnnaEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> {
			enemy.addModifier(StatModifier.freezeAttack(ModifierSource.EFFECT, ctx.self().side(),
					ModifierDuration.rounds(1)));
			parts.add("freezes " + enemy.name() + ": its attack is 0 in the first round");
		});
		EffectActions.log(ctx, parts);
	}
}
