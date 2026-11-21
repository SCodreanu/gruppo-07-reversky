package it.downfall.reversky.engine.core.effect;

import java.util.List;

import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.Penalty;

/**
 * Effetto di una Hidden: la carta resta coperta nella zona Hidden del proprietario e si attiva una volta sola, poi si
 * scarta (D-15).
 * <p>
 * Le Hidden senza condizione si attivano all'inizio del prossimo scontro del proprietario (ENG-20, {@link #trigger}).
 * Quelle con una condizione ({@link #condition()}, ENG-37) restano coperte finché la condizione si verifica: il motore
 * chiama allora {@link #cancelPenalties} (Remnants of Reality) o {@link #preventLethalAttack} (Unseen Exit). Anche
 * loro, se vengono dalla pescata iniziale, non si attivano nel primo scontro.
 */
public non-sealed interface HiddenEffect extends CardEffect {

	/**
	 * Quando si attiva la Hidden.
	 *
	 * @return la condizione; {@link HiddenCondition#NONE} per l'inizio del prossimo scontro
	 */
	default HiddenCondition condition() {
		return HiddenCondition.NONE;
	}

	/**
	 * Attiva una Hidden senza condizione, all'inizio dello scontro.
	 *
	 * @param ctx contesto con la partita, il proprietario e la carta
	 * @throws IllegalStateException per una Hidden con condizione, che non si attiva così
	 */
	default void trigger(EffectContext ctx) {
		throw new IllegalStateException("Hidden con condizione " + condition() + ": " + code());
	}

	/**
	 * La propria creatura ha ricevuto penalità ({@link HiddenCondition#PENALTY}): la Hidden le può annullare.
	 *
	 * @param ctx       contesto con la partita, il proprietario e la carta
	 * @param creature  creatura del proprietario, anche con DEF 0 o meno
	 * @param penalties penalità ricevute dopo l'ultimo controllo, nell'ordine; la prima ha attivato la Hidden
	 * @throws IllegalStateException se la Hidden non ha questa condizione
	 */
	default void cancelPenalties(EffectContext ctx, CreatureInPlay creature, List<Penalty> penalties) {
		throw new IllegalStateException("Hidden senza condizione PENALTY: " + code());
	}

	/**
	 * Un attacco avversario ucciderebbe la propria creatura ({@link HiddenCondition#LETHAL_ATTACK}), al passo 3: la
	 * Hidden lo può ridurre.
	 *
	 * @param ctx      contesto con la partita, il proprietario e la carta
	 * @param creature creatura del proprietario
	 * @param damage   danno dell'attacco dopo le altre riduzioni
	 * @return il danno da applicare, almeno 0
	 * @throws IllegalStateException se la Hidden non ha questa condizione
	 */
	default int preventLethalAttack(EffectContext ctx, CreatureInPlay creature, int damage) {
		throw new IllegalStateException("Hidden senza condizione LETHAL_ATTACK: " + code());
	}
}
