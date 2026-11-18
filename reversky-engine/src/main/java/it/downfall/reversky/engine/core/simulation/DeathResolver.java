package it.downfall.reversky.engine.core.simulation;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PlayerState;
import it.downfall.reversky.engine.core.model.Side;

/**
 * Controllo delle morti con le regole del passo 7 di docs/effetti (regole-base, "Esito del round").
 * <p>
 * Si usa dopo <b>ogni</b> fase in cui la DEF di una creatura può scendere, non solo dopo i danni del round (D-08):
 * una Spell, una Hidden, un Terreno o un effetto possono uccidere in qualunque momento, anche fuori da un round.
 * Le regole sono le stesse ovunque:
 * <ul>
 * <li>una creatura con DEF ≤ 0 lascia il campo e conta come sconfitta per chi la stava affrontando (D-42);</li>
 * <li>prima che lasci il campo scatta il suo {@code onDeath} (es. SCP-2317), poi le morti si ricontrollano: un effetto
 * alla morte può uccidere anche l'altra creatura (docs/effetti, "Effetto alla morte");</li>
 * <li>se ne muore una sola, il suo proprietario perde la parte negativa della DEF (D-03), la sopravvissuta perde i
 * modificatori che duravano fino a fine scontro e poi riceve {@code onKill} (ENG-34);</li>
 * <li>se muoiono entrambe nello stesso controllo, nessun giocatore perde punti vita.</li>
 * </ul>
 * Prima di togliere una creatura, un Badge di Uscita Negata del proprietario la può salvare (D-36): allora non muore,
 * e se l'altra muore lo stesso è una morte singola, con il danno al giocatore.
 * <p>
 * Prima di cercare le morti, e di nuovo dopo gli {@code onDeath}, si riferiscono le ferite e si controllano le soglie
 * della DEF ({@link CreatureEffectService#defenseCheck}, deciso il 18 novembre): così succede dopo ogni fase.
 */
public final class DeathResolver {

	private final ItemService items;
	private final CreatureEffectService creatureEffects;

	/**
	 * Crea il controllo. Non ha stato: la stessa istanza serve qualsiasi partita.
	 *
	 * @param items Oggetti, per il Badge di Uscita Negata e il Fascicolo Sigillato 0
	 */
	public DeathResolver(ItemService items) {
		this.items = items;
		this.creatureEffects = new CreatureEffectService(items);
	}

	/**
	 * Toglie dal campo le creature morte e applica le conseguenze. Prima lo sfidante, poi l'avversario.
	 *
	 * @param state stato della partita; il campo di un giocatore può essere vuoto (es. fuori da un round)
	 * @return {@code true} se è morta almeno una creatura, cioè lo scontro è finito
	 */
	public boolean resolve(MatchState state) {
		// deciso il 18 novembre: prima si riferiscono le ferite e si controllano le soglie, che possono uccidere
		creatureEffects.defenseCheck(state);
		List<CreatureInPlay> dead = new ArrayList<>();
		List<CreatureInPlay> newlyDead = collectNewDeaths(state, dead);
		while (!newlyDead.isEmpty()) {
			dead.addAll(newlyDead);
			creatureEffects.death(state, newlyDead);
			creatureEffects.defenseCheck(state);
			newlyDead = collectNewDeaths(state, dead);
		}
		if (dead.isEmpty()) {
			return false;
		}
		state.endCombat();
		dead.forEach(creature -> removeDeadCreature(state, creature));
		if (dead.size() == 1) {
			CreatureInPlay defeated = dead.getFirst();
			damageOwner(state, defeated);
			state.opponentOf(defeated.side()).creature().ifPresent(survivor -> {
				survivor.endCombat();
				// ENG-34: l'eliminazione, a scontro finito
				creatureEffects.kill(state, survivor, defeated);
			});
		}
		return true;
	}

	/** Le creature in campo con DEF ≤ 0 non ancora contate e non salvate da un Badge, nell'ordine dei lati. */
	private List<CreatureInPlay> collectNewDeaths(MatchState state, List<CreatureInPlay> alreadyDead) {
		List<CreatureInPlay> newlyDead = new ArrayList<>();
		for (Side side : Side.values()) {
			state.player(side).creature()
					.filter(creature -> !alreadyDead.contains(creature))
					.filter(CreatureInPlay::isDead)
					.filter(creature -> !items.saveFromDeath(state, creature))
					.ifPresent(creature -> {
						state.log().creatureDied(creature);
						newlyDead.add(creature);
					});
		}
		return newlyDead;
	}

	private static void removeDeadCreature(MatchState state, CreatureInPlay dead) {
		state.player(dead.side()).removeCreature();
		// D-42: conta per chi la stava affrontando, qualunque sia la causa
		state.opponentOf(dead.side()).recordDefeatedCreature();
	}

	/**
	 * Regolamento base, punto 5 (D-03): il giocatore perde solo la parte negativa della DEF finale della creatura
	 * sconfitta; con DEF esattamente 0 non perde niente e non c'è evento. Vale anche per una morte causata da un
	 * effetto.
	 */
	private static void damageOwner(MatchState state, CreatureInPlay defeated) {
		int damage = Math.max(0, -defeated.defense());
		if (damage == 0) {
			return;
		}
		PlayerState owner = state.player(defeated.side());
		owner.loseLifePoints(damage);
		state.log().playerDamaged(owner.side(), damage, owner.lifePoints());
	}
}
