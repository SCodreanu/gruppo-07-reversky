package it.downfall.reversky.engine.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;

/**
 * Test di {@link MatchResult}: il log restituito all'API è immutabile (CodingRequirements §7).
 */
class MatchResultTest {

	private static final MatchEvent STARTED = new MatchEvent(1, 0, EventType.MATCH_STARTED, null,
			Map.of("startingLifePoints", 30), "Match started");
	private static final MatchEvent ENDED = new MatchEvent(2, 1, EventType.MATCH_ENDED, null,
			Map.of("result", "DRAW"), "Match ended");

	@Test
	@DisplayName("Il log è una copia: aggiungere un evento alla lista originale lascia il log con 1 evento")
	void log_sourceListChangedAfterConstruction_logUnchanged() {
		// Arrange
		List<MatchEvent> source = new ArrayList<>(List.of(STARTED));
		MatchResult result = result(source);

		// Act
		source.add(ENDED);

		// Assert
		assertThat(result.log()).containsExactly(STARTED);
	}

	@Test
	@DisplayName("Il log restituito non si può modificare")
	void log_modifiedFromOutside_throwsUnsupportedOperation() {
		// Arrange: lista modificabile, così il test fallisce se il record la tiene così com'è
		MatchResult result = result(new ArrayList<>(List.of(STARTED)));

		// Act + Assert
		assertThatThrownBy(() -> result.log().add(ENDED))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	private static MatchResult result(List<MatchEvent> log) {
		return new MatchResult(BattleResult.DRAW, EndReason.ROUND_LIMIT, 75,
				new PlayerResult(7, 30, 0), new PlayerResult(12, 30, 0), 42L, "1.0", log);
	}
}
