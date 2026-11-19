package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Riga della matrice non valida quando si chiama il service direttamente: riga {@code null}, campo
 * mancante o {@code defBonus} negativo.
 * <p>
 * Da PUT /admin/faction-bonuses non arriva mai: gli stessi casi li ferma prima la validazione
 * ({@code List<@NotNull @Valid FactionBonusRequest>} e {@code @Min(0)} sul DTO) con un 400.
 */
public class FactionBonusRulesViolatedException extends BusinessException {

	public FactionBonusRulesViolatedException(String detail) {
		super(Category.RULE_VIOLATION, "FACTION_BONUS_RULES_VIOLATED", "Faction bonus rules violated", detail);
	}
}
