package it.downfall.reversky.api.battle.client;

/**
 * Il corpo {@code ProblemDetail} di un errore dell'engine (reversky-engine/docs/api-engine, "Errori"): serve solo a
 * scrivere nel log perché l'engine ha rifiutato la simulazione. I campi in più si ignorano.
 *
 * @param code   es. {@code UNKNOWN_EFFECT}, {@code INVALID_CARD_FACTIONS}, {@code VALIDATION_FAILED}
 * @param detail messaggio in inglese dell'engine, es. "Unknown effect VEIL_OF_ANCHORD on card 96"
 */
public record EngineProblem(String code, String detail) {
}
