package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.HiddenEffect;
import it.downfall.reversky.engine.core.effect.Penalties;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.StatModifier;

/**
 * Static Interference, Hidden {@code DAMAGE} senza condizione: "sospende fino a 5 punti di bonus ATK dell'avv. per
 * tutto lo scontro; se non ne ha, 5 danni all'avv.".
 * <p>
 * Si attiva all'inizio dello scontro, come le altre Hidden senza condizione (D-15). La sospensione è un sigillo fino a
 * fine scontro (ENG-35, deciso il 18 novembre), lo stesso dell'Addetto alle Uscite: vale anche per i bonus ricevuti
 * dopo nello stesso scontro. "Se non ne ha" guarda i bonus ATK effettivi, al netto di altri sigilli; allora i 5 danni
 * sono un danno da Hidden, che passa da {@link Penalties}. Il Fascicolo Sigillato 0 la annulla prima che si attivi,
 * come ogni Hidden {@code DAMAGE}.
 */
public final class StaticInterferenceEffect implements HiddenEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "STATIC_INTERFERENCE";

	private static final int SEAL = 5;
	private static final int DAMAGE = 5;

	/** Crea l'effetto. */
	public StaticInterferenceEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void trigger(EffectContext ctx) {
		CreatureInPlay target = ctx.opponent().creature().orElseThrow(
				() -> new IllegalStateException("Hidden DAMAGE senza creatura avversaria"));
		if (target.effectiveAttackBonus() > 0) {
			target.addModifier(StatModifier.sealAttack(ModifierSource.HIDDEN, ctx.owner().side(), SEAL,
					ModifierDuration.untilCombatEnd()));
			ctx.log().hiddenTriggered(ctx.owner().side(), ctx.card(), "seals " + SEAL + " points of "
					+ target.name() + "'s ATK bonuses this combat (ATK " + MatchLog.number(target.attack()) + ")");
			return;
		}
		int dealt = Penalties.defense(ctx.state(), target, DAMAGE, ctx.owner().side(), null);
		ctx.log().hiddenTriggered(ctx.owner().side(), ctx.card(), MatchLog.signed(-dealt) + " DEF to "
				+ target.name());
	}
}
