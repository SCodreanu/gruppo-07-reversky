package it.downfall.reversky.engine.core.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.effect.HiddenCondition;
import it.downfall.reversky.engine.core.effect.HiddenEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.Penalty;
import it.downfall.reversky.engine.core.model.PlayerState;
import it.downfall.reversky.engine.core.model.SetHidden;
import it.downfall.reversky.engine.core.model.Side;

/**
 * Hidden con una condizione (D-15, ENG-37): restano coperte nella zona Hidden finché la condizione si verifica, poi si
 * attivano una volta e si scartano. Si controlla solo la Hidden attiva del proprietario, se è pronta: quelle in fila
 * aspettano che si liberi il posto (ENG-41). Una Hidden della pescata iniziale non si attiva nel primo scontro. Il
 * Campanello del Torii dell'avversario la può annullare.
 * <ul>
 * <li>{@link HiddenCondition#PENALTY} (Remnants of Reality): al controllo dopo ogni fase, se la creatura ha ricevuto
 * penalità ({@link #penalties}, deciso il 18 novembre).</li>
 * <li>{@link HiddenCondition#LETHAL_ATTACK} (Unseen Exit): al passo 3, se l'attacco avversario ucciderebbe la
 * creatura ({@link #lethalAttack}).</li>
 * </ul>
 * Una zona Hidden incrociata dall'Operatore della Stanza 0 (ENG-39, deciso il 19 novembre) lavora per l'avversario:
 * finché l'incrocio è in attesa, le sue Hidden con condizione si controllano sulla creatura dell'avversario, dopo le
 * Hidden di quella creatura, e non aiutano più la creatura del loro proprietario. La prima che si attiva usa
 * l'incrocio.
 */
public final class HiddenConditionService {

	private final EffectRegistry effects;
	private final ItemService items;

	/**
	 * Crea il servizio. Non conserva stato tra le partite.
	 *
	 * @param effects registro degli effetti
	 * @param items   Oggetti, per il Campanello del Torii
	 */
	public HiddenConditionService(EffectRegistry effects, ItemService items) {
		this.effects = effects;
		this.items = items;
	}

	/**
	 * Controllo delle penalità dopo una fase: per ogni creatura in campo, prima lo sfidante, prende le penalità in coda
	 * e, se il proprietario ha una Hidden {@link HiddenCondition#PENALTY} pronta, la attiva. La creatura può avere
	 * DEF 0 o meno: la Hidden può evitarle la morte.
	 *
	 * @param state stato della partita
	 */
	public void penalties(MatchState state) {
		for (Side side : Side.values()) {
			PlayerState owner = state.player(side);
			Optional<CreatureInPlay> creature = owner.creature().filter(CreatureInPlay::hasUnreportedPenalties);
			if (creature.isEmpty()) {
				continue;
			}
			List<Penalty> penalties = creature.get().takeUnreportedPenalties();
			activate(state, owner, HiddenCondition.PENALTY).ifPresent(
					found -> found.effect().cancelPenalties(found.context(), creature.get(), penalties));
		}
	}

	/**
	 * Passo 3, dopo le prevenzioni dell'effetto: se l'attacco ucciderebbe la creatura (danno almeno pari alla sua DEF,
	 * scudi compresi), il proprietario attiva la sua Hidden {@link HiddenCondition#LETHAL_ATTACK} pronta, anche se non
	 * basta a salvarla.
	 *
	 * @param state    stato della partita
	 * @param defender creatura colpita
	 * @param damage   danno dell'attacco, almeno 0
	 * @return il danno da applicare, almeno 0
	 */
	public int lethalAttack(MatchState state, CreatureInPlay defender, int damage) {
		if (damage <= 0 || damage < defender.defense()) {
			return damage;
		}
		return activate(state, state.player(defender.side()), HiddenCondition.LETHAL_ATTACK)
				.map(found -> Math.max(0, found.effect().preventLethalAttack(found.context(), defender, damage)))
				.orElse(damage);
	}

	/**
	 * Una Hidden si sta attivando dalla zona del suo proprietario (ENG-39, deciso il 19 novembre): se la zona ha un
	 * incrocio in attesa lo usa, lo scrive nel log e la Hidden lavora per l'avversario. Da qui in poi tutto (Oggetti
	 * che la annullano, contesto, log) guarda il proprietario effettivo.
	 *
	 * @param state  stato della partita
	 * @param owner  proprietario della zona da cui si attiva
	 * @param hidden la Hidden
	 * @return il lato per cui lavora la Hidden
	 */
	static Side effectiveOwner(MatchState state, PlayerState owner, CardDefinition hidden) {
		Optional<String> crossing = owner.takeCrossing();
		if (crossing.isEmpty()) {
			return owner.side();
		}
		Side crossed = state.opponentOf(owner.side()).side();
		state.log().effectTriggered(crossed, crossing.get(), "crosses the lines: " + hidden.name() + " works for the "
				+ MatchLog.player(crossed));
		return crossed;
	}

	/**
	 * Cerca la prima Hidden pronta con quella condizione per la creatura di un giocatore: prima nella sua zona, se non
	 * è incrociata, poi in quella dell'avversario, se lo è (ENG-39). La scarta; se il Campanello del Torii
	 * dell'avversario del proprietario effettivo la annulla lo dice nel log.
	 *
	 * @param beneficiary giocatore la cui creatura soddisfa la condizione
	 * @return la Hidden da attivare, o vuoto se non ce n'è una o è stata annullata
	 */
	private Optional<Found> activate(MatchState state, PlayerState beneficiary, HiddenCondition condition) {
		PlayerState enemy = state.opponentOf(beneficiary.side());
		List<PlayerState> zones = new ArrayList<>();
		if (beneficiary.pendingCrossings().isEmpty()) {
			zones.add(beneficiary);
		}
		if (!enemy.pendingCrossings().isEmpty()) {
			zones.add(enemy);
		}
		int combat = state.combatsStarted();
		for (PlayerState zone : zones) {
			for (SetHidden hidden : zone.hiddenZone()) {
				HiddenEffect effect = effects.createHidden(hidden.card().effectCode());
				if (!hidden.isReadyIn(combat) || effect.condition() != condition) {
					continue;
				}
				// ENG-41: la Hidden in fila diventa attiva dallo scontro dopo
				zone.discardHidden(hidden, combat + 1);
				Side owner = effectiveOwner(state, zone, hidden.card());
				if (items.cancelEnemyHidden(state, state.opponentOf(owner), hidden.card())) {
					state.log().hiddenTriggered(owner, hidden.card(), "cancelled");
					return Optional.empty();
				}
				return Optional.of(new Found(effect, new EffectContext(state, owner, hidden.card())));
			}
		}
		return Optional.empty();
	}

	/** Hidden da attivare, con il suo contesto. */
	private record Found(HiddenEffect effect, EffectContext context) {
	}
}
