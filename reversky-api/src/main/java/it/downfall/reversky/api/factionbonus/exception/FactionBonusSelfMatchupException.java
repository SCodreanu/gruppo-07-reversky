package it.downfall.reversky.api.factionbonus.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Una fazione è stata abbinata a se stessa (origin = target). Corrisponde al vincolo
 * {@code chk_faction_bonuses_no_self_matchup}: nel database è CHECK, qui diventa 422.
 */
public class FactionBonusSelfMatchupException extends BusinessException {

	public FactionBonusSelfMatchupException(Integer factionId) {
		super(Category.RULE_VIOLATION, "FACTION_BONUS_SELF_MATCHUP",
			"A faction cannot be matched against itself",
			"Faction " + factionId + " is used both as origin and as target");
	}
}