package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * L'Addetto alle Uscite (The Exit Clerk), Uncommon (Theory of the Wound), <i>Indicazioni cancellate</i>: "all'entrata
 * l'avv. perde fino a 2 punti dei suoi bonus ATK finché resta in campo, compresi quelli che riceve dopo; se non ne ha,
 * −1 ATK nel primo round".
 * <p>
 * Il sigillo (ENG-35, deciso il 18 novembre) resta sull'avversaria per tutta la sua permanenza in campo, anche dopo
 * la morte dell'Addetto, e si mette sempre. "Se non ne ha" guarda i bonus ATK effettivi, già al netto di altri
 * sigilli: allora c'è anche il −1 ATK per il primo round.
 */
public final class ExitClerkEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "EXIT_CLERK";

	private static final int SEAL = 2;
	private static final int NO_BONUS_PENALTY = -1;

	/** Crea l'effetto. */
	public ExitClerkEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> {
			boolean noBonus = enemy.effectiveAttackBonus() == 0;
			parts.add(EffectActions.seal(enemy, ctx.self(), SEAL, ModifierDuration.whileInPlay()));
			if (noBonus) {
				parts.add(EffectActions.attack(enemy, ctx.self(), NO_BONUS_PENALTY, ModifierDuration.rounds(1),
						"in the first round"));
			}
		});
		EffectActions.log(ctx, parts);
	}
}
