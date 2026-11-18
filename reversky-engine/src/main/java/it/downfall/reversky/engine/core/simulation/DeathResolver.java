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
 * <li>se ne muore una sola, il suo proprietario perde la parte negativa della DEF (D-03) e la sopravvissuta perde i
 * modificatori che duravano fino a fine scontro;</li>
 * <li>se muoiono entrambe nello stesso controllo, nessun giocatore perde punti vita.</li>
 * </ul>
 */
public final class DeathResolver {

	/**
	 * Crea il controllo. Non ha stato: la stessa istanza serve qualsiasi partita.
	 */
	public DeathResolver() {
	}

	/**
	 * Toglie dal campo le creature morte e applica le conseguenze. Prima lo sfidante, poi l'avversario.
	 *
	 * @param state stato della partita; il campo di un giocatore può essere vuoto (es. fuori da un round)
	 * @return {@code true} se è morta almeno una creatura, cioè lo scontro è finito
	 */
	public boolean resolve(MatchState state) {
		List<CreatureInPlay> dead = new ArrayList<>();
		for (Side side : Side.values()) {
			state.player(side).creature().filter(CreatureInPlay::isDead).ifPresent(dead::add);
		}
		if (dead.isEmpty()) {
			return false;
		}
		dead.forEach(creature -> removeDeadCreature(state, creature));
		if (dead.size() == 1) {
			CreatureInPlay defeated = dead.getFirst();
			damageOwner(state, defeated);
			state.opponentOf(defeated.side()).creature().ifPresent(CreatureInPlay::endCombat);
		}
		return true;
	}

	private static void removeDeadCreature(MatchState state, CreatureInPlay dead) {
		state.log().creatureDied(dead);
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
