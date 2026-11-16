package it.downfall.reversky.engine.core.rules;

/**
 * Regole di una partita, costruite dal blocco {@code rules} della richiesta (rule set attivo nel database
 * dell'API). Nessun valore predefinito: tutto arriva dalla richiesta (docs/regole-base, "Valori").
 *
 * @param rulesVersion       versione del rule set, restituita uguale nel risultato
 * @param startingLifePoints punti vita iniziali di ogni giocatore
 * @param maxRounds          limite di round totali della partita
 */
public record RuleConfig(String rulesVersion, int startingLifePoints, int maxRounds) {
}
