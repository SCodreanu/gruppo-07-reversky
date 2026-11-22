package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.CreatureInPlay;

/**
 * Wendigo, Legendary (Theory of the Invitation), <i>Insaziabile</i>: "a fine round avv. −4 DEF più 1 per ogni nemica
 * eliminata da Wendigo, massimo 7; ogni eliminazione fa recuperare 5 DEF, fino alla DEF all'entrata". Erano −3 e
 * massimo 6; cambiati con ENG-42 (proposta del 21 novembre).
 * <p>
 * Il danno di fine round è un danno da effetto (D-08) e il Fascicolo Sigillato 0 lo può annullare. Un'eliminazione è
 * ogni morte dell'avversaria, per qualunque causa ({@code onKill}, D-42), anche per il suo danno di fine round. Stato:
 * le eliminazioni dall'entrata in campo.
 */
public final class WendigoEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "WENDIGO";

	private static final int BASE_DAMAGE = 4;
	private static final int MAX_DAMAGE = 7;
	private static final int KILL_HEAL = 5;

	private int kills;

	/** Crea l'effetto. */
	public WendigoEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onRoundEnd(CreatureContext ctx) {
		int damage = Math.min(MAX_DAMAGE, BASE_DAMAGE + kills);
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> parts.add(EffectActions.damage(ctx, enemy, damage)));
		EffectActions.log(ctx, parts);
	}

	@Override
	public void onKill(CreatureContext ctx, CreatureInPlay defeated) {
		kills++;
		EffectActions.heal(ctx.self(), KILL_HEAL, " after defeating " + defeated.name())
				.ifPresent(part -> EffectActions.log(ctx, List.of(part)));
	}
}
