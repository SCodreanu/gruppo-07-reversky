package it.downfall.reversky.engine.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Una riga di {@code faction_bonuses} (docs/fazioni). Che i valori abbiano senso (fazioni diverse, {@code defBonus}
 * non negativo, nessuna coppia ripetuta) lo controlla il core: qui basta che ci siano.
 *
 * @param originFactionId fazione che riceve il bonus
 * @param targetFactionId fazione avversaria
 * @param atkBonus        bonus di attacco, negativo se è un malus
 * @param defBonus        bonus di difesa
 */
public record FactionBonusRequest(

		@NotNull(message = "Origin faction id is required")
		Integer originFactionId,

		@NotNull(message = "Target faction id is required")
		Integer targetFactionId,

		@NotNull(message = "ATK bonus is required")
		Integer atkBonus,

		@NotNull(message = "DEF bonus is required")
		Integer defBonus) {
}
