package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;

/**
 * Skin-Stealers, Rare (Theory of the Wound), <i>Pelle rubata</i>: "all'entrata ottiene DEF pari alla metà della DEF
 * base avversaria (per difetto, massimo 4) finché resta in campo".
 * <p>
 * DEF base = la DEF della carta avversaria, non quella attuale. È DEF permanente e non colpisce l'avversaria, quindi
 * il Fascicolo Sigillato 0 non la ferma.
 */
public final class SkinStealersEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SKIN_STEALERS";

	private static final int MAX_DEFENSE = 4;

	/** Crea l'effetto. */
	public SkinStealersEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		List<String> parts = new ArrayList<>();
		ctx.enemy().ifPresent(enemy -> {
			int stolen = Math.min(MAX_DEFENSE, enemy.card().defense() / 2);
			if (stolen > 0) {
				parts.add("takes the skin of " + enemy.name() + ": " + EffectActions.defense(ctx.self(), stolen));
			}
		});
		EffectActions.log(ctx, parts);
	}
}
