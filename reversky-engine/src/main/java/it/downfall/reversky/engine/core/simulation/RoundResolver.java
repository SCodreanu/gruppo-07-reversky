package it.downfall.reversky.engine.core.simulation;

import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PlayerState;
import it.downfall.reversky.engine.core.rules.FactionMatrix;
import it.downfall.reversky.engine.core.rules.RuleConfig;

/**
 * Risolve l'inizio di uno scontro e i round, nell'ordine dei passi di docs/effetti ("Ordine di risoluzione di un
 * round"), che fa parte delle regole: non si cambia senza aggiornare quel documento.
 * <p>
 * Passi già presenti: 0 (bonus DEF di fazione), 2 (calcolo dell'attacco), 4 (danni simultanei) e 7 (morti e danno
 * al giocatore). Gli altri passi arrivano con gli effetti, le Spell, le Hidden, i Terreni e gli Oggetti
 * (ENG-19…ENG-23). Quando una cosa riguarda entrambi i giocatori, prima lo sfidante e poi l'avversario.
 */
public final class RoundResolver {

	private final RuleConfig rules;
	private final FactionMatrix factionMatrix;

	/**
	 * Crea il risolutore per una partita.
	 *
	 * @param rules         regole della partita
	 * @param factionMatrix bonus di fazione della partita
	 */
	public RoundResolver(RuleConfig rules, FactionMatrix factionMatrix) {
		this.rules = rules;
		this.factionMatrix = factionMatrix;
	}

	/**
	 * Regole con cui è stato creato il risolutore.
	 *
	 * @return le regole
	 */
	public RuleConfig rules() {
		return rules;
	}

	/**
	 * Passo 0, inizio di uno scontro: una nuova coppia di creature si affronta. Si chiama dopo che una creatura è
	 * entrata in campo, anche se l'altra era già in campo.
	 * <p>
	 * Ogni creatura riceve il bonus DEF di fazione contro la nuova avversaria (docs/fazioni): una volta per scontro,
	 * come DEF permanente. Alla fine si fissa la DEF all'entrata delle creature al loro primo scontro (M-06).
	 *
	 * @param state stato della partita, con due creature in campo
	 * @throws IllegalStateException se manca una creatura
	 */
	public void startCombat(MatchState state) {
		CreatureInPlay challenger = requireCreature(state.challenger());
		CreatureInPlay opponent = requireCreature(state.opponent());
		applyDefenseBonus(challenger, opponent, state.log());
		applyDefenseBonus(opponent, challenger, state.log());
		challenger.recordInitialDefense();
		opponent.recordInitialDefense();
	}

	/**
	 * Risolve un round tra le due creature in campo (passi 1–7).
	 * <p>
	 * Se muore almeno una creatura lo scontro finisce: le morte lasciano il campo e la sopravvissuta perde i
	 * modificatori che duravano fino a fine scontro. Pescare la creatura successiva è compito del simulatore.
	 *
	 * @param state stato della partita, con due creature in campo
	 * @throws IllegalStateException se manca una creatura
	 */
	public void resolve(MatchState state) {
		CreatureInPlay challenger = requireCreature(state.challenger());
		CreatureInPlay opponent = requireCreature(state.opponent());
		MatchLog log = state.log();
		log.roundStarted(state.startNextRound());

		// Passo 2: si calcolano entrambi gli attacchi prima di applicare i danni
		int challengerAttack = computeAttack(challenger, opponent, log);
		int opponentAttack = computeAttack(opponent, challenger, log);

		// Passo 4, regolamento base punto 3: i danni si applicano insieme, nessuno colpisce per primo
		opponent.takeDamage(challengerAttack);
		challenger.takeDamage(opponentAttack);
		log.damageDealt(challenger, opponent, challengerAttack);
		log.damageDealt(opponent, challenger, opponentAttack);

		endRound(challenger);
		endRound(opponent);
		resolveDeaths(state, challenger, opponent);
	}

	private void applyDefenseBonus(CreatureInPlay creature, CreatureInPlay against, MatchLog log) {
		int bonus = factionMatrix.defBonus(creature.factions(), against.factions());
		if (bonus > 0) {
			creature.addDefense(bonus);
			log.effectTriggered(creature.side(), creature.name(),
					"+" + bonus + " DEF faction bonus (DEF " + creature.defense() + ")");
		}
	}

	/** Attacco effettivo: ATK con i modificatori più il bonus di fazione, mai sotto 0 (D-04). */
	private int computeAttack(CreatureInPlay attacker, CreatureInPlay defender, MatchLog log) {
		int factionBonus = factionMatrix.atkBonus(attacker.factions(), defender.factions());
		int total = Math.max(0, attacker.attack() + factionBonus);
		log.attackComputed(attacker, factionBonus, total);
		return total;
	}

	/** Fine del round dopo il passo 6: scadono i modificatori a round delle creature ancora vive. */
	private static void endRound(CreatureInPlay creature) {
		if (!creature.isDead()) {
			creature.endRound();
		}
	}

	/** Passo 7: morti, poi danno al giocatore solo se è morta una sola creatura (regole-base, "Esito del round"). */
	private static void resolveDeaths(MatchState state, CreatureInPlay challenger, CreatureInPlay opponent) {
		boolean challengerDied = challenger.isDead();
		boolean opponentDied = opponent.isDead();
		if (!challengerDied && !opponentDied) {
			return;
		}
		if (challengerDied) {
			removeDeadCreature(state, challenger);
		}
		if (opponentDied) {
			removeDeadCreature(state, opponent);
		}
		if (challengerDied != opponentDied) {
			CreatureInPlay defeated = challengerDied ? challenger : opponent;
			damageOwner(state, defeated);
			CreatureInPlay survivor = challengerDied ? opponent : challenger;
			survivor.endCombat();
		}
	}

	private static void removeDeadCreature(MatchState state, CreatureInPlay dead) {
		state.log().creatureDied(dead);
		state.player(dead.side()).removeCreature();
		// D-42: conta per chi la stava affrontando, qualunque sia la causa
		state.opponentOf(dead.side()).recordDefeatedCreature();
	}

	/**
	 * Regolamento base, punto 5 (D-03): il giocatore perde solo la parte negativa della DEF finale della creatura
	 * sconfitta; con DEF esattamente 0 non perde niente e non c'è evento.
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

	private static CreatureInPlay requireCreature(PlayerState player) {
		return player.creature().orElseThrow(
				() -> new IllegalStateException(player.side() + " non ha una creatura in campo"));
	}
}
