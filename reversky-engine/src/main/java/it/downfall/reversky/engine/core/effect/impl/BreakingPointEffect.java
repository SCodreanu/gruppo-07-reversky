package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.HiddenEffect;
import it.downfall.reversky.engine.core.effect.Penalties;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;

/**
 * Breaking Point, Hidden {@code DAMAGE}, <i>Spezzata</i>: "se l'avv. ha 7 DEF o meno, la sua DEF scende a 0 e muore,
 * senza danni al giocatore; altrimenti −4 DEF" (catalogo degli effetti, numeri del bilanciamento del 13 novembre).
 * <p>
 * La DEF che conta è quella attuale, scudi compresi. Portarla a 0 è un danno da effetto pari a tutta la DEF (D-49):
 * con DEF esattamente 0 la creatura muore (D-16) e il suo proprietario non perde punti vita (D-03).
 */
public final class BreakingPointEffect implements HiddenEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "BREAKING_POINT";

	private static final int BREAKING_THRESHOLD = 7;
	private static final int DAMAGE = 4;

	/** Crea l'effetto. */
	public BreakingPointEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void trigger(EffectContext ctx) {
		CreatureInPlay target = ctx.opponent().creature().orElseThrow(
				() -> new IllegalStateException("Hidden DAMAGE senza creatura avversaria"));
		int defense = target.defense();
		int damage = defense <= BREAKING_THRESHOLD ? defense : DAMAGE;
		// il Terreno può ridurre la penalità (Hollow Clearing): allora anche "a 0" diventa una perdita normale
		int dealt = Penalties.defense(ctx.state(), target, damage, ctx.owner().side(), null);
		String effect = dealt == defense ? "DEF of " + target.name() + " to 0"
				: MatchLog.signed(-dealt) + " DEF to " + target.name();
		ctx.log().hiddenTriggered(ctx.owner().side(), ctx.card(), effect);
	}
}
