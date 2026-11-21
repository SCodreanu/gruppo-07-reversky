package it.downfall.reversky.engine.core.rules;

/**
 * Riga della tabella {@code faction_bonuses}: quando una creatura di {@code originFactionId} affronta una
 * creatura di {@code targetFactionId}, ottiene questi bonus (docs/fazioni).
 *
 * @param originFactionId fazione che riceve il bonus
 * @param targetFactionId fazione affrontata
 * @param atkBonus        si somma all'attacco in ogni round dello scontro; può essere negativo (malus, D-04)
 * @param defBonus        si somma alla DEF una volta, all'inizio dello scontro; mai negativo
 */
public record FactionBonus(int originFactionId, int targetFactionId, int atkBonus, int defBonus) {
}
