package it.downfall.reversky.engine.core.model;

/**
 * Il Terreno in gioco e chi l'ha pescato. In gioco c'è un solo Terreno alla volta, di chiunque sia (D-19); il
 * proprietario serve a The Null Witness, che lo rimuove solo se l'ha pescato l'avversario (D-38).
 *
 * @param card  carta del Terreno
 * @param owner lato di chi l'ha pescato
 */
public record PlacedTerrain(CardDefinition card, Side owner) {
}
