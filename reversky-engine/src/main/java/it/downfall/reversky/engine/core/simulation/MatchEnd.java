package it.downfall.reversky.engine.core.simulation;

import it.downfall.reversky.engine.core.model.BattleResult;
import it.downfall.reversky.engine.core.model.EndReason;

/**
 * Esito e motivo della fine di una partita, deciso da {@link EndConditionChecker}.
 *
 * @param result esito
 * @param reason motivo di fine
 */
public record MatchEnd(BattleResult result, EndReason reason) {
}
