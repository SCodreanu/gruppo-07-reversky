package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.Penalties;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.StatModifier;

/**
 * Azioni ripetute dagli effetti delle creature, con il testo del log in inglese. Ogni azione restituisce la sua parte
 * di messaggio; l'effetto le unisce in un solo {@code EFFECT_TRIGGERED} con {@link #log(CreatureContext, List)}.
 */
final class EffectActions {

	private EffectActions() {
	}

	/**
	 * Danno da effetto (D-08): perdita permanente di DEF, gli scudi assorbono per primi. La causa della ferita è la
	 * creatura che porta l'effetto; il Terreno in gioco può ridurre la penalità ({@link Penalties}). Le morti le
	 * controlla il motore.
	 *
	 * @param ctx    contesto dell'effetto
	 * @param target creatura colpita
	 * @param amount danno, almeno 0
	 * @return la parte di messaggio
	 */
	static String damage(CreatureContext ctx, CreatureInPlay target, int amount) {
		int dealt = Penalties.defense(ctx.state(), target, amount, ctx.self().side(), ctx.self());
		return "deals " + dealt + " damage to " + target.name() + " (DEF " + MatchLog.number(target.defense()) + ")";
	}

	/**
	 * Modificatore di ATK con origine {@code EFFECT} (D-35).
	 *
	 * @param target   creatura che lo riceve
	 * @param cause    creatura che lo causa
	 * @param amount   ATK da sommare, negativo per un malus
	 * @param duration durata
	 * @param when     quando vale, per il log (es. "while in play"), o vuoto
	 * @return la parte di messaggio
	 */
	static String attack(CreatureInPlay target, CreatureInPlay cause, int amount, ModifierDuration duration,
			String when) {
		target.addModifier(StatModifier.attack(ModifierSource.EFFECT, cause.side(), amount, duration));
		String self = target == cause ? "" : target.name() + " ";
		String suffix = when.isEmpty() ? "" : " " + when;
		return self + MatchLog.signed(amount) + " ATK" + suffix + " (ATK " + MatchLog.number(target.attack()) + ")";
	}

	/**
	 * DEF permanente in più (es. Il Raccoglitore di Orme): resta finché la creatura è in campo e i danni la consumano.
	 *
	 * @param target creatura che la riceve
	 * @param amount DEF in più, almeno 0
	 * @return la parte di messaggio
	 */
	static String defense(CreatureInPlay target, int amount) {
		target.addDefense(amount);
		return MatchLog.signed(amount) + " DEF (DEF " + MatchLog.number(target.defense()) + ")";
	}

	/**
	 * Registra le parti in un solo evento, se ce ne sono.
	 *
	 * @param ctx   contesto dell'effetto
	 * @param parts parti di messaggio, nell'ordine in cui sono avvenute
	 */
	static void log(CreatureContext ctx, List<String> parts) {
		if (!parts.isEmpty()) {
			ctx.log().effectTriggered(ctx.self().side(), ctx.self().name(), String.join(", ", parts));
		}
	}
}
