package it.downfall.reversky.engine.core.effect;

/**
 * Effetto di una Spell: si risolve una volta, quando la carta viene pescata, poi la carta si scarta (D-10).
 * <p>
 * Le Spell {@code DAMAGE} colpiscono la creatura avversaria in campo, le {@code SHIELD} e le {@code BURN} preparano
 * la prossima creatura del proprietario (D-10, D-11, D-13, D-34): lo decide l'effetto, il motore lo chiama soltanto.
 */
public non-sealed interface SpellEffect extends CardEffect {

	/**
	 * Risolve la Spell appena pescata.
	 *
	 * @param ctx contesto con la partita, il proprietario e la carta
	 */
	void resolve(EffectContext ctx);
}
