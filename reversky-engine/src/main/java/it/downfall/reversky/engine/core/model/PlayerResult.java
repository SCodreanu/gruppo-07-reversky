package it.downfall.reversky.engine.core.model;

/**
 * Risultato di un giocatore alla fine della partita (docs/api-engine, risposta).
 *
 * @param userId            id dell'utente, uguale a quello ricevuto
 * @param finalLifePoints   punti vita finali; possono essere negativi (D-43)
 * @param creaturesDefeated creature avversarie morte mentre affrontavano questo giocatore, per qualunque causa;
 *                          in una morte simultanea contano per entrambi (D-42)
 */
public record PlayerResult(int userId, int finalLifePoints, int creaturesDefeated) {
}
