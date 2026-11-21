package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.SpellEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;

/**
 * SCP-109 · Acqua Infinita, Spell {@code SHIELD}, <i>Borraccia</i>: "+2 DEF; una volta, quando la creatura scende a
 * metà DEF o meno, recupera 3 DEF (fino alla DEF all'entrata)".
 * <p>
 * Come le altre Spell {@code SHIELD}, il +2 DEF è uno scudo in attesa della prossima creatura del proprietario (D-10,
 * D-34, D-35). Il modificatore collega alla creatura un effetto (ENG-34, deciso il 18 novembre) che controlla la
 * soglia "metà DEF" dopo ogni fase ({@code onDefenseCheck}, M-06) e recupera una volta sola. È l'effetto di una Spell,
 * non della creatura: annullare l'effetto della creatura non lo tocca.
 */
public final class Scp109Effect implements SpellEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SCP_109";

	private static final int DEFENSE_BONUS = 2;
	private static final int HEAL = 3;

	/** Crea l'effetto. */
	public Scp109Effect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void resolve(EffectContext ctx) {
		String card = ctx.card().name();
		PendingModifier flask = new PendingModifier(card, ModifierSource.SPELL, ctx.owner().side(), 0, DEFENSE_BONUS,
				0, new FlaskEffect(card));
		ctx.owner().addPendingModifier(flask);
		ctx.log().spellResolved(ctx.owner().side(), ctx.card(),
				MatchLog.describe(flask) + ", recovers " + HEAL + " DEF once at half DEF", null);
	}

	/** Effetto collegato alla creatura che riceve la Spell: una nuova istanza per ogni Spell risolta. */
	private static final class FlaskEffect implements CreatureEffect {

		private final String card;
		private boolean used;

		private FlaskEffect(String card) {
			this.card = card;
		}

		@Override
		public String code() {
			return CODE;
		}

		@Override
		public void onDefenseCheck(CreatureContext ctx) {
			CreatureInPlay self = ctx.self();
			if (used || self.defense() * 2 > self.initialDefense()) {
				return;
			}
			used = true;
			EffectActions.heal(self, HEAL, " from " + card).ifPresent(part -> EffectActions.log(ctx, List.of(part)));
		}
	}
}
