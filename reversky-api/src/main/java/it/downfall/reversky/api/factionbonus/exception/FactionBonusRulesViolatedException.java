package it.downfall.reversky.api.factionbonus.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Regola di gioco violata (es. {@code defBonus} negativo chiamando il service direttamente).
 * Sul controller il DTO con {@code @Min(0)} la intercetta già come 400.
 */
public class FactionBonusRulesViolatedException extends BusinessException {

	public FactionBonusRulesViolatedException(String detail) {
		super(Category.RULE_VIOLATION, "FACTION_BONUS_RULES_VIOLATED", "Faction bonus rules violated", detail);
	}
}