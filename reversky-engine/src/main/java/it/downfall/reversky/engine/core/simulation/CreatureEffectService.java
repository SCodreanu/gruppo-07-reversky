package it.downfall.reversky.engine.core.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.effect.EffectPriority;
import it.downfall.reversky.engine.core.effect.EnemyEffectGuard;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PlacedTerrain;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.model.Wound;

/**
 * Chiama gli effetti delle creature nei momenti del round (docs/effetti, "Ordine di risoluzione di un round").
 * <p>
 * Quando più effetti agiscono nello stesso momento, prima la priorità ({@link EffectPriority#NULLIFY} prima di
 * {@link EffectPriority#NORMAL}), poi la creatura dello sfidante (D-30). Ogni chiamata ha il suo
 * {@link CreatureContext}; prima di colpire la creatura avversaria il contesto chiede a {@link ItemService} se un
 * Fascicolo Sigillato 0 annulla l'attivazione. Le morti le controlla chi chiama (D-08).
 * <p>
 * Per ogni creatura si chiama prima il suo effetto, poi quelli collegati da altre carte, nell'ordine in cui sono
 * arrivati (SCP-109, ENG-34); la priorità D-30 la decide l'effetto della creatura.
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
	 * Passo 0, dopo le Hidden: {@code onEnterPlay} delle creature che non sono ancora entrate (una volta per entrata,
	 * D-31), poi {@code onCombatStart} di entrambe. Dopo ogni effetto si controllano le morti, come per le Hidden
	 * (deciso il 18 novembre): se una creatura muore lo scontro finisce lì, e chi non è ancora entrato entra allo
	 * scontro dopo.
	 *
	 * @param state  stato della partita, con due creature in campo
	 * @param deaths controllo delle morti
	 * @return {@code true} se è morta una creatura, cioè lo scontro è finito
	 */
	public boolean enterPlay(MatchState state, DeathResolver deaths) {
		for (CreatureInPlay creature : inOrder(inPlay(state))) {
			if (!creature.hasEntered()) {
				creature.recordEntered();
				creature.effects().forEach(effect -> effect.onEnterPlay(context(state, creature)));
				if (deaths.resolve(state)) {
					return true;
				}
			}
		}
		for (CreatureInPlay creature : inOrder(inPlay(state))) {
			creature.effects().forEach(effect -> effect.onCombatStart(context(state, creature)));
			if (deaths.resolve(state)) {
				return true;
			}
		}
		return false;
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
		int modified = attack;
		for (CreatureEffect effect : attacker.effects()) {
			modified = effect.modifyOutgoingAttack(context(state, attacker), modified);
		}
		return modified;
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
		int modified = damage;
		for (CreatureEffect effect : defender.effects()) {
			modified = Math.max(0, effect.modifyIncomingDamage(context(state, defender), modified));
		}
		return modified;
	}

	/**
	 * Passo 5: {@code afterDamage} di ogni creatura colpita, anche se ha DEF 0 o meno, poi il suo {@code afterAttack}
	 * se il suo attacco ha fatto danni (ENG-34).
	 *
	 * @param state   stato della partita
	 * @param damages danno dell'attacco ricevuto da ogni creatura, indicizzato per {@link Side#ordinal()}
	 * @param wounds  ferita di ogni creatura, indicizzata per {@link Side#ordinal()}
	 */
	public void afterDamage(MatchState state, int[] damages, int[] wounds) {
		forEachEffect(state, inPlay(state), (effect, ctx) -> {
			Side side = ctx.self().side();
			effect.afterDamage(ctx, damages[side.ordinal()], wounds[side.ordinal()]);
			int dealt = damages[ctx.opponent().side().ordinal()];
			if (dealt > 0) {
				effect.afterAttack(ctx, dealt);
			}
		});
	}

	/**
	 * Controllo della DEF dopo una fase (deciso il 18 novembre): per ogni creatura viva, nell'ordine D-30, prima
	 * {@code onWounded} per ogni ferita in coda, seguito da quello del Terreno in gioco se la creatura è della sua
	 * fazione (Echo Rift, ENG-27), poi {@code onDefenseCheck}. Se intanto qualcuno subisce nuove ferite
	 * (es. il danno di The Unseen Matriarch) si ripete. Le ferite di una creatura con DEF 0 o meno restano in coda: se
	 * un Badge di Uscita Negata la salva, arrivano al controllo dopo.
	 * <p>
	 * Lo chiamano il {@link DeathResolver}, prima di cercare le morti e dopo gli {@code onDeath}, e il round al passo
	 * 5, dopo {@code afterDamage}.
	 *
	 * @param state stato della partita
	 */
	public void defenseCheck(MatchState state) {
		do {
			for (CreatureInPlay creature : inOrder(alive(state))) {
				Optional<PlacedTerrain> terrain = state.terrain().filter(placed -> placed.affects(creature));
				for (Wound wound : creature.takeUnreportedWounds()) {
					creature.effects().forEach(effect -> effect.onWounded(context(state, creature), wound));
					// Echo Rift (ENG-27): dopo l'effetto della creatura
					terrain.ifPresent(placed -> placed.effect().onWounded(state, placed, creature, wound));
				}
			}
			for (CreatureInPlay creature : inOrder(alive(state))) {
				// un controllo prima può averla uccisa
				for (CreatureEffect effect : creature.effects()) {
					if (!creature.isDead()) {
						effect.onDefenseCheck(context(state, creature));
					}
				}
			}
		} while (alive(state).stream().anyMatch(CreatureInPlay::hasUnreportedWounds));
	}

	/**
	 * Passo 6: {@code onRoundEnd} delle creature con DEF sopra 0.
	 *
	 * @param state stato della partita
	 */
	public void roundEnd(MatchState state) {
		forEachEffect(state, alive(state), CreatureEffect::onRoundEnd);
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

	/**
	 * Fine di uno scontro con una sola morte (ENG-34): {@code onKill} della sopravvissuta, dopo che l'avversaria ha
	 * lasciato il campo e i modificatori fino a fine scontro sono scaduti.
	 *
	 * @param state    stato della partita
	 * @param survivor creatura sopravvissuta
	 * @param defeated creatura eliminata, già fuori dal campo
	 */
	public void kill(MatchState state, CreatureInPlay survivor, CreatureInPlay defeated) {
		forEachEffect(state, List.of(survivor), (effect, ctx) -> effect.onKill(ctx, defeated));
	}

	private void forEachEffect(MatchState state, List<CreatureInPlay> creatures,
			BiConsumer<CreatureEffect, CreatureContext> moment) {
		for (CreatureInPlay creature : inOrder(creatures)) {
			creature.effects().forEach(effect -> moment.accept(effect, context(state, creature)));
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

	private static List<CreatureInPlay> alive(MatchState state) {
		return inPlay(state).stream().filter(creature -> !creature.isDead()).toList();
	}

	private static List<CreatureInPlay> inPlay(MatchState state) {
		List<CreatureInPlay> creatures = new ArrayList<>();
		for (Side side : Side.values()) {
			state.player(side).creature().ifPresent(creatures::add);
		}
		return creatures;
	}
}
