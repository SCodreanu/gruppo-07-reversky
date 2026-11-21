package it.downfall.reversky.api.deck.exception;

import java.util.List;
import java.util.Map;

import it.downfall.reversky.api.common.exception.BusinessException;
import it.downfall.reversky.api.deck.dto.DeckViolation;

/**
 * Il mazzo non rispetta le regole e non viene salvato (docs/mazzi): la risposta elenca tutte le violazioni in
 * {@code violations}, così il frontend le mostra insieme.
 */
public class InvalidDeckException extends BusinessException {

	private final List<DeckViolation> violations;

	public InvalidDeckException(List<DeckViolation> violations) {
		super(Category.RULE_VIOLATION, "DECK_INVALID", "Invalid deck",
			"The deck breaks " + violations.size() + " rule(s): see violations");
		this.violations = List.copyOf(violations);
	}

	public List<DeckViolation> getViolations() {
		return violations;
	}

	@Override
	public Map<String, Object> getExtraProperties() {
		return Map.of("violations", violations);
	}
}
