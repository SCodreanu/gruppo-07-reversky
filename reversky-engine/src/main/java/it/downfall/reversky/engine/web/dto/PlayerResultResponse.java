package it.downfall.reversky.engine.web.dto;

/**
 * Risultato di un giocatore (docs/api-engine).
 *
 * @param userId            id dell'utente
 * @param finalLifePoints   punti vita finali, anche negativi (D-43)
 * @param creaturesDefeated creature avversarie morte mentre affrontavano questo giocatore, per qualunque causa (D-42)
 */
public record PlayerResultResponse(int userId, int finalLifePoints, int creaturesDefeated) {
}
