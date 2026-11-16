package it.downfall.reversky.engine.core.rules;

import it.downfall.reversky.engine.core.validation.ErrorCode;
import it.downfall.reversky.engine.core.validation.InvalidSimulationInputException;

/**
 * Regole di una partita, costruite dal blocco {@code rules} della richiesta (rule set attivo nel database
 * dell'API). Nessun valore predefinito: tutto arriva dalla richiesta (docs/regole-base, "Valori").
 *
 * @param rulesVersion       versione del rule set, restituita uguale nel risultato
 * @param startingLifePoints punti vita iniziali di ogni giocatore, almeno 1
 * @param maxRounds          limite di round totali della partita, almeno 1
 */
public record RuleConfig(String rulesVersion, int startingLifePoints, int maxRounds) {

	/**
	 * Rifiuta regole con valori minori di 1: con 0 punti vita la partita sarebbe già finita, con 0 round non
	 * inizierebbe. Il codice è lo stesso che dà la Bean Validation nello strato {@code web} (docs/api-engine,
	 * controllo 1, esempio 95), così il core usato da solo (test, bilanciamento) si comporta come la REST.
	 *
	 * @throws InvalidSimulationInputException con {@link ErrorCode#VALIDATION_FAILED}
	 */
	public RuleConfig {
		if (startingLifePoints < 1 || maxRounds < 1) {
			throw new InvalidSimulationInputException(ErrorCode.VALIDATION_FAILED,
					"Regole non valide: punti vita iniziali " + startingLifePoints + ", limite di round " + maxRounds
							+ " (minimo 1)");
		}
	}
}
