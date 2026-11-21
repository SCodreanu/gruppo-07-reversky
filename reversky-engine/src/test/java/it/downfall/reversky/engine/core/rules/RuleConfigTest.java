package it.downfall.reversky.engine.core.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.validation.ErrorCode;
import it.downfall.reversky.engine.core.validation.InvalidSimulationInputException;

/**
 * Test di {@link RuleConfig}: regole con valori minori di 1 non si possono costruire (docs/regole-base, "Valori";
 * docs/api-engine, controllo 1, esempio 95).
 */
class RuleConfigTest {

	@Test
	@DisplayName("Il minimo ammesso, 1 punto vita e 1 round, è valido")
	void constructor_minimumValues_accepted() {
		assertThatCode(() -> new RuleConfig("1.0", 1, 1)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("0 punti vita iniziali → VALIDATION_FAILED")
	void constructor_zeroLifePoints_validationFailed() {
		assertThatThrownBy(() -> new RuleConfig("1.0", 0, 75))
				.isInstanceOfSatisfying(InvalidSimulationInputException.class,
						e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
	}

	@Test
	@DisplayName("Versione mancante, vuota o di soli spazi → VALIDATION_FAILED: va salvata con ogni partita")
	void constructor_missingVersion_validationFailed() {
		for (String version : new String[] { null, "", "  " }) {
			assertThatThrownBy(() -> new RuleConfig(version, 30, 75))
					.isInstanceOfSatisfying(InvalidSimulationInputException.class,
							e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
		}
	}

	@Test
	@DisplayName("Limite di 0 round (esempio 95) → VALIDATION_FAILED")
	void constructor_zeroMaxRounds_validationFailed() {
		assertThatThrownBy(() -> new RuleConfig("1.0", 30, 0))
				.isInstanceOfSatisfying(InvalidSimulationInputException.class,
						e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
	}
}
