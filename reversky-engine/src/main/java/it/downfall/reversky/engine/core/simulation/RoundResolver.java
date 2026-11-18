package it.downfall.reversky.engine.core.simulation;

import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PendingModifier;
import it.downfall.reversky.engine.core.model.PlayerState;
import it.downfall.reversky.engine.core.rules.FactionMatrix;

/**
 * Risolve l'inizio di uno scontro e i round, nell'ordine dei passi di docs/effetti ("Ordine di risoluzione di un
 * round"), che fa parte delle regole: non si cambia senza aggiornare quel documento.
 * <p>
 * Passi già presenti: 0 (bonus DEF di fazione e modificatori in attesa), 2 (calcolo dell'attacco), 4 (danni
 * simultanei) e 7 (morti e danno al giocatore, con il {@link DeathResolver}). Gli altri passi arrivano con gli
 * effetti, le Hidden, i Terreni e gli Oggetti (ENG-20…ENG-23). Quando una cosa riguarda entrambi i giocatori, prima
 * lo sfidante e poi l'avversario.
 */
public final class RoundResolver {

	private final FactionMatrix factionMatrix;
	private final DeathResolver deaths = new DeathResolver();

	/**
	 * Crea il risolutore per una partita.
	 *
	 * @param factionMatrix bonus di fazione della partita
	 */
	public RoundResolver(FactionMatrix factionMatrix) {
		this.factionMatrix = factionMatrix;
	}

	/**
	 * Passo 0, inizio di uno scontro: una nuova coppia di creature si affronta. Si chiama dopo che una creatura è
	 * entrata in campo, anche se l'altra era già in campo.
	 * <ol>
	 * <li>Ogni creatura riceve il bonus DEF di fazione contro la nuova avversaria (docs/fazioni): una volta per
	 * scontro, come DEF permanente.</li>
	 * <li>Ogni creatura al suo primo scontro riceve i modificatori in attesa del proprietario (D-34), prima lo
	 * sfidante.</li>
	 * <li>Si controllano le morti (D-08): un danno in attesa può uccidere, e allora lo scontro finisce prima di
	 * cominciare.</li>
	 * <li>Si fissa la DEF all'entrata delle creature rimaste, al loro primo scontro (M-06).</li>
	 * </ol>
	 *
	 * @param state stato della partita, con due creature in campo
	 * @throws IllegalStateException se manca una creatura
	 */
	public void startCombat(MatchState state) {
		CreatureInPlay challenger = requireCreature(state.challenger());
		CreatureInPlay opponent = requireCreature(state.opponent());
		applyDefenseBonus(challenger, opponent, state.log());
		applyDefenseBonus(opponent, challenger, state.log());
		applyPendingModifiers(state.challenger(), challenger, state.log());
		applyPendingModifiers(state.opponent(), opponent, state.log());
		deaths.resolve(state);
		state.challenger().creature().ifPresent(CreatureInPlay::recordInitialDefense);
		state.opponent().creature().ifPresent(CreatureInPlay::recordInitialDefense);
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
		// Passo 7 (D-08): lo stesso controllo vale dopo ogni fase che toglie DEF. Quando arriveranno i passi 1–3
		// (ENG-23), una morte prima del passo 4 interrompe il round: il round conta, nessuno attacca e i
		// modificatori a round della sopravvissuta scadono come a fine round (docs/effetti)
		deaths.resolve(state);
	}

	private void applyDefenseBonus(CreatureInPlay creature, CreatureInPlay against, MatchLog log) {
		int bonus = factionMatrix.defBonus(creature.factions(), against.factions());
		if (bonus > 0) {
			creature.addDefense(bonus);
			log.effectTriggered(creature.side(), creature.name(),
					"+" + bonus + " DEF faction bonus (DEF " + creature.defense() + ")");
		}
	}

	/** D-34: i modificatori in attesa vanno solo a una creatura appena entrata, mai a una sopravvissuta. */
	private static void applyPendingModifiers(PlayerState owner, CreatureInPlay creature, MatchLog log) {
		if (creature.hasStartedCombat()) {
			return;
		}
		for (PendingModifier modifier : owner.takePendingModifiers()) {
			modifier.applyTo(creature);
			log.effectTriggered(creature.side(), creature.name(), MatchLog.describe(modifier) + " from "
					+ modifier.card() + " (ATK " + MatchLog.number(creature.attack()) + ", DEF "
					+ MatchLog.number(creature.defense()) + ")");
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

	private static CreatureInPlay requireCreature(PlayerState player) {
		return player.creature().orElseThrow(
				() -> new IllegalStateException(player.side() + " non ha una creatura in campo"));
	}
}
