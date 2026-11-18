package it.downfall.reversky.engine.core.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.effect.EffectPriority;
import it.downfall.reversky.engine.core.effect.EnemyEffectGuard;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.Side;

/**
 * Chiama gli effetti delle creature nei momenti del round (docs/effetti, "Ordine di risoluzione di un round").
 * <p>
 * Quando più effetti agiscono nello stesso momento, prima la priorità ({@link EffectPriority#NULLIFY} prima di
 * {@link EffectPriority#NORMAL}), poi la creatura dello sfidante (D-30). Ogni chiamata ha il suo
 * {@link CreatureContext}; prima di colpire la creatura avversaria il contesto chiede a {@link ItemService} se un
 * Fascicolo Sigillato 0 annulla l'attivazione. Le morti le controlla chi chiama (D-08).
 */
public final class CreatureEffectService {

	private final EnemyEffectGuard guard;

	/**
	 * Crea il servizio. Non conserva stato tra le partite: lo stato degli effetti sta nelle creature.
	 *
	 * @param items Oggetti, per il Fascicolo Sigillato 0
	 */
	public CreatureEffectService(ItemService items) {
		this.guard = (state, source, target) -> items.cancelEnemyEffect(state, state.player(target.side()),
				source.name(), target.name());
	}

	/**
	 * Passo 0: {@code onEnterPlay} delle creature in campo che non sono ancora entrate, una volta per entrata (D-31).
	 *
	 * @param state stato della partita
	 */
	public void enterPlay(MatchState state) {
		for (CreatureInPlay creature : inOrder(inPlay(state))) {
			if (!creature.hasEntered()) {
				creature.recordEntered();
				creature.effect().ifPresent(effect -> effect.onEnterPlay(context(state, creature)));
			}
		}
	}

	/**
	 * Passo 1: {@code onRoundStart} delle due creature.
	 *
	 * @param state stato della partita
	 */
	public void roundStart(MatchState state) {
		forEachEffect(state, inPlay(state), CreatureEffect::onRoundStart);
	}

	/**
	 * Passo 2: l'effetto dell'attaccante modifica il suo attacco.
	 *
	 * @param state    stato della partita
	 * @param attacker creatura che attacca
	 * @param attack   attacco con il bonus di fazione e i modificatori
	 * @return l'attacco modificato, prima del minimo 0
	 */
	public int outgoingAttack(MatchState state, CreatureInPlay attacker, int attack) {
		return attacker.effect()
				.map(effect -> effect.modifyOutgoingAttack(context(state, attacker), attack))
				.orElse(attack);
	}

	/**
	 * Passo 3: l'effetto della creatura colpita modifica il danno dell'attacco che riceve.
	 *
	 * @param state    stato della partita
	 * @param defender creatura colpita
	 * @param damage   danno dell'attacco, almeno 0
	 * @return il danno da applicare, almeno 0
	 */
	public int incomingDamage(MatchState state, CreatureInPlay defender, int damage) {
		int modified = defender.effect()
				.map(effect -> effect.modifyIncomingDamage(context(state, defender), damage))
				.orElse(damage);
		return Math.max(0, modified);
	}

	/**
	 * Passo 5: {@code afterDamage} di ogni creatura colpita, anche se ha DEF 0 o meno.
	 *
	 * @param state  stato della partita
	 * @param wounds ferita di ogni creatura in campo, indicizzata per {@link Side#ordinal()}
	 */
	public void afterDamage(MatchState state, int[] wounds) {
		forEachEffect(state, inPlay(state),
				(effect, ctx) -> effect.afterDamage(ctx, wounds[ctx.self().side().ordinal()]));
	}

	/**
	 * Passo 6: {@code onRoundEnd} delle creature con DEF sopra 0.
	 *
	 * @param state stato della partita
	 */
	public void roundEnd(MatchState state) {
		forEachEffect(state, inPlay(state).stream().filter(creature -> !creature.isDead()).toList(),
				CreatureEffect::onRoundEnd);
	}

	/**
	 * Controllo delle morti: {@code onDeath} delle creature appena morte, prima che lascino il campo.
	 *
	 * @param state stato della partita
	 * @param dead  creature morte, ancora in campo
	 */
	public void death(MatchState state, List<CreatureInPlay> dead) {
		forEachEffect(state, dead, CreatureEffect::onDeath);
	}

	private void forEachEffect(MatchState state, List<CreatureInPlay> creatures,
			BiConsumer<CreatureEffect, CreatureContext> moment) {
		for (CreatureInPlay creature : inOrder(creatures)) {
			creature.effect().ifPresent(effect -> moment.accept(effect, context(state, creature)));
		}
	}

	private CreatureContext context(MatchState state, CreatureInPlay creature) {
		return new CreatureContext(state, creature, guard);
	}

	/** D-30: prima la priorità, poi lo sfidante. L'ordinamento è stabile e la lista parte dallo sfidante. */
	private static List<CreatureInPlay> inOrder(List<CreatureInPlay> creatures) {
		List<CreatureInPlay> ordered = new ArrayList<>(creatures);
		ordered.sort(Comparator.comparing((CreatureInPlay creature) -> creature.effect()
						.map(CreatureEffect::priority).orElse(EffectPriority.NORMAL))
				.thenComparing(CreatureInPlay::side));
		return ordered;
	}

	private static List<CreatureInPlay> inPlay(MatchState state) {
		List<CreatureInPlay> creatures = new ArrayList<>();
		for (Side side : Side.values()) {
			state.player(side).creature().ifPresent(creatures::add);
		}
		return creatures;
	}
}
