package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.SpellEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;

/**
 * SCP-458 · Pizza Senza Fine, Spell {@code SHIELD}, <i>Fette da dividere</i>: "+2 DEF alla prossima creatura e +2 DEF a
 * quella dopo".
 * <p>
 * Due modificatori in attesa in fila (D-34): uno per la prossima creatura del proprietario e uno per quella dopo. Come
 * le altre Spell {@code SHIELD}, la DEF è uno scudo che dura finché la creatura resta in campo (D-12, D-35).
 */
public final class Scp458Effect implements SpellEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SCP_458";

	private static final int DEFENSE_BONUS = 2;
	private static final int CREATURES = 2;

	/** Crea l'effetto. */
	public Scp458Effect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void resolve(EffectContext ctx) {
		PendingModifier slice = new PendingModifier(ctx.card().name(), ModifierSource.SPELL, ctx.owner().side(), 0,
				DEFENSE_BONUS, 0);
		for (int position = 0; position < CREATURES; position++) {
			ctx.owner().addPendingModifier(position, slice);
		}
		ctx.log().spellResolved(ctx.owner().side(), ctx.card(),
				MatchLog.describe(slice) + " to the next " + CREATURES + " creatures", null);
	}
}
