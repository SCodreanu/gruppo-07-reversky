package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.effect.EffectPriority;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * Il Predicatore sotto la Pioggia (The Rain Preacher), Rare (Theory of the Invitation), <i>Sermone che cancella</i>:
 * "all'entrata annulla l'effetto dell'avv. finché resta in campo; se non ha effetto, ha invece −2 ATK finché resta in
 * campo".
 * <p>
 * Priorità {@code NULLIFY} (D-30): se entrano insieme, annulla anche l'effetto di entrata avversario. L'annullamento
 * resta all'avversaria per tutta la sua permanenza, anche dopo la morte del Predicatore (ENG-38, deciso il 19
 * novembre). "Non ha effetto" vuol dire che la carta non ha un codice: contro un effetto già annullato non dà il −2.
 */
public final class RainPreacherEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "RAIN_PREACHER";

	private static final int PENALTY = -2;

	/** Crea l'effetto. */
	public RainPreacherEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public EffectPriority priority() {
		return EffectPriority.NULLIFY;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> {
			if (!EffectActions.hasEffect(enemy)) {
				parts.add(EffectActions.attack(enemy, ctx.self(), PENALTY, ModifierDuration.whileInPlay(),
						"while in play"));
			} else if (enemy.cancelEffect(ModifierDuration.whileInPlay())) {
				parts.add(EffectActions.cancel(enemy));
			}
		});
		EffectActions.log(ctx, parts);
	}
}
