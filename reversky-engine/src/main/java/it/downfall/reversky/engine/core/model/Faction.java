package it.downfall.reversky.engine.core.model;

/**
 * Fazione ricevuta nella richiesta (tabella {@code factions} dell'API). Le fazioni sono dati e non un enum:
 * si modificano dal backoffice (docs/fazioni).
 *
 * @param id   id della fazione nel database dell'API
 * @param name nome mostrato ai giocatori, in inglese (es. "Theory of the Wound")
 */
public record Faction(int id, String name) {
}
