package it.downfall.reversky.engine.core.simulation;

import java.util.List;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.effect.HiddenCondition;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;
import it.downfall.reversky.engine.core.model.PlayerState;
import it.downfall.reversky.engine.core.model.SetHidden;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.rules.FactionMatrix;

/**
 * Risolve l'inizio di uno scontro e i round, nell'ordine dei passi di docs/effetti ("Ordine di risoluzione di un
 * round"), che fa parte delle regole: non si cambia senza aggiornare quel documento.
 * <p>
 * Ci sono tutti i passi, da 0 a 7; gli effetti delle creature li chiama {@link CreatureEffectService} (ENG-23) e le
 * morti le controlla il {@link DeathResolver}. Quando una cosa riguarda entrambi i giocatori, prima lo sfidante e poi
 * l'avversario; per gli effetti delle creature prima la priorità, poi lo sfidante (D-30).
 */
public final class RoundResolver {

	/** D-15: se più Hidden si attivano insieme, prima le {@code DAMAGE}, poi le {@code SURVIVAL}. */
	private static final List<CardSubtype> HIDDEN_ORDER = List.of(CardSubtype.DAMAGE, CardSubtype.SURVIVAL);

	private final FactionMatrix factionMatrix;
	private final EffectRegistry effects;
	private final TerrainService terrains;
	private final ItemService items;
	private final CreatureEffectService creatureEffects;
	private final DeathResolver deaths;
	private final HiddenConditionService hiddenConditions;

	/**
	 * Crea il risolutore per una partita.
	 *
	 * @param factionMatrix bonus di fazione della partita
	 * @param effects       registro degli effetti, per le Hidden che si attivano a inizio scontro
	 */
	public RoundResolver(FactionMatrix factionMatrix, EffectRegistry effects) {
		this.factionMatrix = factionMatrix;
		this.effects = effects;
		this.terrains = new TerrainService(effects);
		this.items = new ItemService(effects);
		this.creatureEffects = new CreatureEffectService(items);
		this.deaths = new DeathResolver(items);
		this.hiddenConditions = new HiddenConditionService(effects, items);
	}

	/**
	 * Passo 0, inizio di uno scontro: una nuova coppia di creature si affronta. Si chiama dopo che una creatura è
	 * entrata in campo, anche se l'altra era già in campo.
	 * <ol>
	 * <li>Ogni creatura riceve il bonus DEF di fazione contro la nuova avversaria (docs/fazioni): una volta per
	 * scontro, come DEF permanente.</li>
	 * <li>Ogni creatura al suo primo scontro riceve i modificatori in attesa del proprietario (D-34), prima lo
	 * sfidante.</li>
	 * <li>Ogni creatura della fazione del Terreno in gioco che non ha ancora il suo bonus lo riceve (D-17), prima lo
	 * sfidante.</li>
	 * <li>Una Bussola dei Sentieri Falsi del proprietario programma gli scudi a inizio round della sua creatura,
	 * dopo il Terreno (D-48).</li>
	 * <li>Si attivano le Hidden pronte per questo scontro (D-15): prima le {@code DAMAGE}, poi le {@code SURVIVAL};
	 * a parità, prima quelle dello sfidante, nell'ordine in cui sono state pescate (D-30).</li>
	 * <li>{@code onEnterPlay} delle creature che non sono ancora entrate (D-31), poi {@code onCombatStart} di
	 * entrambe; dopo ognuno si controllano le morti.</li>
	 * <li>Si fissa la DEF all'entrata delle creature appena entrate (M-06).</li>
	 * <li>Effetti secondari dei Terreni a inizio scontro (Devouring Abyss, ENG-27), prima lo sfidante, con il controllo
	 * delle morti dopo ognuno.</li>
	 * </ol>
	 * Dopo ogni fase che può togliere DEF si controllano le morti (D-08): se muore una creatura lo scontro finisce
	 * prima di cominciare, e le Hidden non ancora attivate restano coperte per lo scontro dopo. Una creatura che non è
	 * arrivata al suo {@code onEnterPlay} entra allo scontro dopo (deciso il 18 novembre).
	 *
	 * @param state stato della partita, con due creature in campo
	 * @throws IllegalStateException se manca una creatura
	 */
	public void startCombat(MatchState state) {
		CreatureInPlay challenger = requireCreature(state.challenger());
		CreatureInPlay opponent = requireCreature(state.opponent());
		int combat = state.startNextCombat();
		applyDefenseBonus(challenger, opponent, state.log());
		applyDefenseBonus(opponent, challenger, state.log());
		applyPendingModifiers(state, state.challenger(), challenger);
		applyPendingModifiers(state, state.opponent(), opponent);
		boolean ended = deaths.resolve(state);
		if (!ended) {
			terrains.applyBonus(state, challenger);
			terrains.applyBonus(state, opponent);
			items.startCombat(state, challenger);
			items.startCombat(state, opponent);
			ended = triggerHiddenCards(state, combat) || creatureEffects.enterPlay(state, deaths);
		}
		state.challenger().creature().ifPresent(RoundResolver::recordCombatStarted);
		state.opponent().creature().ifPresent(RoundResolver::recordCombatStarted);
		if (!ended) {
			// effetti secondari dei Terreni a inizio scontro (Devouring Abyss), dopo la DEF all'entrata
			terrains.startCombat(state, deaths);
		}
	}

	/** Fine del passo 0: la DEF all'entrata si fissa solo per una creatura che è entrata davvero (M-06). */
	private static void recordCombatStarted(CreatureInPlay creature) {
		creature.recordCombatStarted();
		if (creature.hasEntered()) {
			creature.recordInitialDefense();
		}
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

		// Passo 1: scudi a inizio round (Bussola dei Sentieri Falsi), poi onRoundStart
		items.startRound(state, challenger);
		items.startRound(state, opponent);
		creatureEffects.roundStart(state);
		if (deaths.resolve(state)) {
			// D-08: una morte prima del passo 4 interrompe il round; il round conta, nessuno attacca e i modificatori
			// a round della sopravvissuta scadono come a fine round (deciso il 17 novembre)
			state.challenger().creature().ifPresent(RoundResolver::endRound);
			state.opponent().creature().ifPresent(RoundResolver::endRound);
			return;
		}

		// Passo 2: si calcolano entrambi gli attacchi prima di applicare i danni
		int challengerAttack = computeAttack(state, challenger, opponent);
		int opponentAttack = computeAttack(state, opponent, challenger);

		// Passo 3: le prevenzioni della creatura colpita, prima lo sfidante
		int toChallenger = creatureEffects.incomingDamage(state, challenger, opponentAttack);
		int toOpponent = creatureEffects.incomingDamage(state, opponent, challengerAttack);
		// ENG-37: dopo tutte le altre riduzioni, le Hidden contro un attacco letale (Unseen Exit)
		toChallenger = hiddenConditions.lethalAttack(state, challenger, toChallenger);
		toOpponent = hiddenConditions.lethalAttack(state, opponent, toOpponent);

		// Passo 4, regolamento base punto 3: i danni si applicano insieme, nessuno colpisce per primo
		int[] damages = new int[Side.values().length];
		int[] wounds = new int[Side.values().length];
		damages[Side.OPPONENT.ordinal()] = toOpponent;
		damages[Side.CHALLENGER.ordinal()] = toChallenger;
		wounds[Side.OPPONENT.ordinal()] = opponent.takeDamage(toOpponent, challenger);
		wounds[Side.CHALLENGER.ordinal()] = challenger.takeDamage(toChallenger, opponent);
		log.damageDealt(challenger, opponent, toOpponent);
		log.damageDealt(opponent, challenger, toChallenger);

		// Passi 5 e 6: afterDamage di entrambe, poi le ferite e le soglie della DEF (deciso il 18 novembre), onRoundEnd
		// solo delle vive
		creatureEffects.afterDamage(state, damages, wounds);
		deaths.checkDefense(state);
		creatureEffects.roundEnd(state);

		endRound(challenger);
		endRound(opponent);
		// Passo 7 (D-08): lo stesso controllo vale dopo ogni fase che toglie DEF
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
	private void applyPendingModifiers(MatchState state, PlayerState owner, CreatureInPlay creature) {
		MatchLog log = state.log();
		if (creature.hasStartedCombat()) {
			return;
		}
		for (PendingModifier modifier : owner.takePendingModifiers()) {
			if (modifier.side() != creature.side() && modifier.source() == ModifierSource.EFFECT
					&& creature.isImmuneToEnemyCreatureEffects()) {
				// ENG-38: Il Custode delle Porte Incrociate, prima del Fascicolo Sigillato 0 che così non si consuma
				continue;
			}
			if (modifier.side() != creature.side()
					&& items.cancelEnemyEffect(state, owner, modifier.card(), creature.name())) {
				continue;
			}
			modifier.applyTo(state, creature);
			log.effectTriggered(creature.side(), creature.name(), MatchLog.describe(modifier) + " from "
					+ modifier.card() + " (ATK " + MatchLog.number(creature.attack()) + ", DEF "
					+ MatchLog.number(creature.defense()) + ")");
		}
	}

	/**
	 * D-15: le Hidden senza condizione pronte per questo scontro, {@code DAMAGE} prima di {@code SURVIVAL}. Ogni Hidden
	 * si scarta quando si attiva; se una uccide, le altre aspettano lo scontro dopo.
	 *
	 * @return {@code true} se una Hidden ha ucciso, cioè lo scontro è finito
	 */
	private boolean triggerHiddenCards(MatchState state, int combat) {
		for (CardSubtype subtype : HIDDEN_ORDER) {
			for (Side side : Side.values()) {
				PlayerState owner = state.player(side);
				for (SetHidden hidden : owner.hiddenZone()) {
					if (hidden.isReadyIn(combat) && hidden.card().subtype() == subtype
							&& effects.createHidden(hidden.card().effectCode()).condition() == HiddenCondition.NONE) {
						owner.discardHidden(hidden);
						if (isCancelled(state, side, hidden.card())) {
							state.log().hiddenTriggered(side, hidden.card(), "cancelled");
							continue;
						}
						effects.createHidden(hidden.card().effectCode())
								.trigger(new EffectContext(state, side, hidden.card()));
						if (deaths.resolve(state)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Un Oggetto dell'avversario può annullare la Hidden prima del suo effetto: il Campanello del Torii ogni Hidden, il
	 * Fascicolo Sigillato 0 una Hidden {@code DAMAGE}, che colpirebbe la sua creatura.
	 */
	private boolean isCancelled(MatchState state, Side owner, CardDefinition hidden) {
		PlayerState target = state.opponentOf(owner);
		if (items.cancelEnemyHidden(state, target, hidden)) {
			return true;
		}
		return hidden.subtype() == CardSubtype.DAMAGE && items.cancelEnemyEffect(state, target, hidden.name(),
				target.creature().map(CreatureInPlay::name).orElse(null));
	}

	/**
	 * Attacco effettivo: ATK con i modificatori più il bonus di fazione, poi l'effetto dell'attaccante
	 * ({@code modifyOutgoingAttack}), mai sotto 0 (D-04). Un attacco congelato (Yuki-onna) vale 0, dopo tutto il
	 * resto (deciso il 18 novembre).
	 */
	private int computeAttack(MatchState state, CreatureInPlay attacker, CreatureInPlay defender) {
		int factionBonus = factionMatrix.atkBonus(attacker.factions(), defender.factions());
		int attack = attacker.attack() + factionBonus;
		int effectBonus = creatureEffects.outgoingAttack(state, attacker, attack) - attack;
		boolean frozen = attacker.isAttackFrozen();
		int total = frozen ? 0 : Math.max(0, attack + effectBonus);
		state.log().attackComputed(attacker, factionBonus, effectBonus, frozen, total);
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
