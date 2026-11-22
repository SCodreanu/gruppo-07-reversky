package it.downfall.reversky.engine.core.effect.impl;

import java.util.Optional;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.SpellEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;

/**
 * Base delle Spell {@code BURN} (D-13, <i>Nessuna carta persa</i>): rivelano la prossima carta del mazzo del
 * proprietario.
 * <ul>
 * <li>Se è una creatura, è proprio la prossima creatura che verrà pescata: entra con il bonus fisso della Burn.</li>
 * <li>Se non è una creatura, non si scarta nulla e la carta resta al suo posto; alcune Burn danno comunque un bonus
 * minore alla prossima creatura.</li>
 * <li>Con il mazzo vuoto non succede nulla.</li>
 * </ul>
 * Il bonus è un modificatore in attesa (D-34) e dura finché la creatura resta in campo (D-12). Evento
 * {@code BURN_REVEALED}, poi {@code SPELL_RESOLVED} con il bonus dato.
 */
public abstract class BurnSpellEffect implements SpellEffect {

	private static final String NO_EFFECT = "no effect";

	private final int creatureAttack;
	private final int creatureDefense;
	private final int otherAttack;
	private final int otherDefense;

	/**
	 * Crea la base.
	 *
	 * @param creatureAttack  ATK in più se la carta rivelata è una creatura
	 * @param creatureDefense DEF in più se la carta rivelata è una creatura
	 * @param otherAttack     ATK in più alla prossima creatura se la carta rivelata non è una creatura (0 se niente)
	 * @param otherDefense    DEF in più alla prossima creatura se la carta rivelata non è una creatura (0 se niente)
	 */
	protected BurnSpellEffect(int creatureAttack, int creatureDefense, int otherAttack, int otherDefense) {
		this.creatureAttack = creatureAttack;
		this.creatureDefense = creatureDefense;
		this.otherAttack = otherAttack;
		this.otherDefense = otherDefense;
	}

	@Override
	public final void resolve(EffectContext ctx) {
		Optional<CardDefinition> revealed = ctx.owner().peekCard();
		boolean isCreature = revealed.filter(card -> card.type() == CardType.CREATURE).isPresent();
		ctx.log().burnRevealed(ctx.owner().side(), ctx.card(), revealed.orElse(null), isCreature);
		String effect = NO_EFFECT;
		if (revealed.isPresent()) {
			int attack = isCreature ? creatureAttack : otherAttack;
			int defense = isCreature ? creatureDefense : otherDefense;
			if (attack != 0 || defense != 0) {
				PendingModifier bonus = new PendingModifier(ctx.card().name(), ModifierSource.SPELL,
						ctx.owner().side(), attack, defense, 0);
				ctx.owner().addPendingModifier(bonus);
				effect = MatchLog.describe(bonus);
			}
		}
		ctx.log().spellResolved(ctx.owner().side(), ctx.card(), effect, null);
	}
}
