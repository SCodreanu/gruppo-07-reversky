package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.Wound;

/**
 * Shuten-dōji, Legendary (Theory of the Dream): "dopo ogni danno DEF subito +6 ATK cumulativi, massimo +18"
 * (era +4, massimo +12; ENG-42, proposta del 21 novembre).
 * <p>
 * Conta ogni ferita (perdita reale di DEF sopra 0, dopo gli scudi), anche dai danni degli effetti. Ogni +6 è un bonus
 * ATK finché resta in campo, quindi vale anche contro le nuove avversarie: una ferita del passo 4 conta dall'attacco
 * del round dopo, una ferita all'entrata già dal primo round.
 */
public final class ShutenDojiEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SHUTEN_DOJI";

	private static final int ATTACK_PER_WOUND = 6;
	private static final int MAX_BONUS = 18;

	private int bonus;

	/** Crea l'effetto. */
	public ShutenDojiEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onWounded(CreatureContext ctx, Wound wound) {
		int gained = Math.min(ATTACK_PER_WOUND, MAX_BONUS - bonus);
		if (gained <= 0) {
			return;
		}
		bonus += gained;
		EffectActions.log(ctx, List.of(EffectActions.attack(ctx.self(), ctx.self(), gained,
				ModifierDuration.whileInPlay(), "while in play")));
	}
}
