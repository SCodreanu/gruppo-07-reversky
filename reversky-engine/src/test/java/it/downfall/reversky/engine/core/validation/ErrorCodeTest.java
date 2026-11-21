package it.downfall.reversky.engine.core.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * I codici di errore sono il campo {@code code} del {@code ProblemDetail}: l'API li legge per nome. Questo test li
 * controlla contro la tabella "Errori" di docs/api-engine, nello stesso ordine in cui si controllano.
 */
class ErrorCodeTest {

	@Test
	@DisplayName("ErrorCode ha i 9 codici di docs/api-engine, nell'ordine dei controlli")
	void errorCode_matchesContractInOrder() {
		assertThat(Arrays.stream(ErrorCode.values()).map(Enum::name)).containsExactly(
				"VALIDATION_FAILED", "UNKNOWN_FACTION", "INVALID_FACTION_BONUS", "INVALID_CARD_FACTIONS",
				"NO_CREATURES_IN_DECK", "INVALID_STATS", "MISSING_SUBTYPE", "MISSING_EFFECT", "UNKNOWN_EFFECT");
	}
}
