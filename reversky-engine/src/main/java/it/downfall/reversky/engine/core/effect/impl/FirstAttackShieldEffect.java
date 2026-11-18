package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.CreatureInPlay;

/**
 * Base degli effetti "previene N danni dal primo attacco di ogni creatura avversaria" (docs/effetti/catalogo-effetti,
 * classi base, D-37): Hidebehind, Nurikabe, Sala d'Attesa Vivente e, con altre parti, The Rootbound Doppelganger e
 * The Stitched Corridor.
 * <p>
 * Conta il primo attacco con danno sopra 0 di ogni avversaria (D-33, stato "ultima avversaria"): un attacco da 0 non
 * consuma la prevenzione, e una nuova avversaria la fa ripartire. La prevenzione non scende sotto 0.
 */
public abstract class FirstAttackShieldEffect implements CreatureEffect {

	private final int prevented;
	/** Ultima avversaria il cui primo attacco è già stato ridotto. */
	private CreatureInPlay usedAgainst;

	/**
	 * Crea la base.
	 *
	 * @param prevented danni prevenuti dal primo attacco di ogni avversaria, almeno 1
	 */
	protected FirstAttackShieldEffect(int prevented) {
		this.prevented = prevented;
	}

	@Override
	public final int modifyIncomingDamage(CreatureContext ctx, int damage) {
		CreatureInPlay attacker = ctx.enemy().orElse(null);
		if (damage <= 0 || attacker == null || attacker == usedAgainst) {
			return damage;
		}
		usedAgainst = attacker;
		int reduced = Math.max(0, damage - prevented);
		ctx.log().effectTriggered(ctx.self().side(), ctx.self().name(),
				"prevents " + (damage - reduced) + " damage from the first attack of " + attacker.name());
		afterFirstAttack(ctx, attacker);
		return reduced;
	}

	/**
	 * Dopo la prevenzione del primo attacco di un'avversaria (passo 3): le sottoclassi aggiungono qui quello che segue
	 * (The Rootbound Doppelganger). Di base non fa niente.
	 *
	 * @param ctx      contesto
	 * @param attacker avversaria il cui primo attacco è stato ridotto
	 */
	protected void afterFirstAttack(CreatureContext ctx, CreatureInPlay attacker) {
	}
}
