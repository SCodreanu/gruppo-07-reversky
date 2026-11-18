package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.CreatureInPlay;

/**
 * L'Ombra del Torii (Torii Shadow), Uncommon (Theory of the Dream): "all'entrata +3 DEF se c'è il Terreno della
 * Theory of the Dream, altrimenti +2, finché resta in campo".
 * <p>
 * Il Terreno della Theory of the Dream è un Terreno della sua stessa fazione (deciso il 18 novembre: le fazioni sono
 * dati della richiesta, il codice non ne conosce gli id). Vale il Terreno di chiunque (D-17), controllato solo
 * all'entrata. È DEF permanente.
 */
public final class ToriiShadowEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "TORII_SHADOW";

	private static final int DEFENSE_WITH_TERRAIN = 3;
	private static final int DEFENSE_WITHOUT_TERRAIN = 2;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		CreatureInPlay self = ctx.self();
		boolean ownTerrain = ctx.state().terrain()
				.map(terrain -> terrain.card().factions().stream().anyMatch(self.factions()::contains))
				.orElse(false);
		int defense = ownTerrain ? DEFENSE_WITH_TERRAIN : DEFENSE_WITHOUT_TERRAIN;
		EffectActions.log(ctx, List.of(EffectActions.defense(self, defense)));
	}
}
