package it.downfall.reversky.engine.core.simulation;

import java.util.List;
import java.util.Optional;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.effect.HiddenCondition;
import it.downfall.reversky.engine.core.effect.HiddenEffect;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.Penalty;
import it.downfall.reversky.engine.core.model.PlayerState;
import it.downfall.reversky.engine.core.model.SetHidden;
import it.downfall.reversky.engine.core.model.Side;

/**
 * Hidden con una condizione (D-15, ENG-37): restano coperte nella zona Hidden finché la condizione si verifica, poi si
 * attivano una volta e si scartano. Si attiva la prima pronta del proprietario, nell'ordine di pescata; una Hidden
 * della pescata iniziale non si attiva nel primo scontro. Il Campanello del Torii dell'avversario la può annullare.
 * <ul>
 * <li>{@link HiddenCondition#PENALTY} (Remnants of Reality): al controllo dopo ogni fase, se la creatura ha ricevuto
 * penalità ({@link #penalties}, deciso il 18 novembre).</li>
 * <li>{@link HiddenCondition#LETHAL_ATTACK} (Unseen Exit): al passo 3, se l'attacco avversario ucciderebbe la
 * creatura ({@link #lethalAttack}).</li>
 * </ul>
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
	 * Scarta la prima Hidden pronta con quella condizione; se il Campanello del Torii la annulla lo dice nel log.
	 *
	 * @return la Hidden da attivare, o vuoto se non ce n'è una o è stata annullata
	 */
	private Optional<Found> activate(MatchState state, PlayerState owner, HiddenCondition condition) {
		int combat = state.combatsStarted();
		for (SetHidden hidden : owner.hiddenZone()) {
			HiddenEffect effect = effects.createHidden(hidden.card().effectCode());
			if (!hidden.isReadyIn(combat) || effect.condition() != condition) {
				continue;
			}
			owner.discardHidden(hidden);
			if (items.cancelEnemyHidden(state, state.opponentOf(owner.side()), hidden.card())) {
				state.log().hiddenTriggered(owner.side(), hidden.card(), "cancelled");
				return Optional.empty();
			}
			return Optional.of(new Found(effect, new EffectContext(state, owner.side(), hidden.card())));
		}
		return Optional.empty();
	}

	/** Hidden da attivare, con il suo contesto. */
	private record Found(HiddenEffect effect, EffectContext context) {
	}
}
