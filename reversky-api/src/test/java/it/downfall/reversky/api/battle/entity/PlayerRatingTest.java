package it.downfall.reversky.api.battle.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.api.battle.dto.BattleOutcome;

/**
 * {@link PlayerRating}: valori iniziali e applicazione di una partita classificata.
 */
class PlayerRatingTest {

	@Test
	@DisplayName("A new rating starts at 1000, peak 1000, no games")
	void new_initialValues() {
		// Act
		PlayerRating rating = new PlayerRating(7);

		// Assert
		assertThat(rating.getRating()).isEqualTo(1000);
		assertThat(rating.getPeakRating()).isEqualTo(1000);
		assertThat(rating.getGamesPlayed()).isZero();
		assertThat(rating.getWins() + rating.getLosses() + rating.getDraws()).isZero();
		assertThat(rating.isNew()).isTrue();
	}

	@Test
	@DisplayName("Each game adds the change, keeps the peak and counts the outcome; games = wins + losses + draws")
	void recordGame_updatesRatingPeakAndCounters() {
		// Arrange
		PlayerRating rating = new PlayerRating(7);

		// Act + Assert: vittoria, sconfitta, pareggio
		rating.recordGame(BattleOutcome.WIN, 13);
		assertThat(rating.getRating()).isEqualTo(1013);
		assertThat(rating.getPeakRating()).isEqualTo(1013);

		rating.recordGame(BattleOutcome.LOSS, -20);
		assertThat(rating.getRating()).isEqualTo(993);
		assertThat(rating.getPeakRating()).isEqualTo(1013);

		rating.recordGame(BattleOutcome.DRAW, 0);
		assertThat(rating.getRating()).isEqualTo(993);
		assertThat(rating.getWins()).isEqualTo(1);
		assertThat(rating.getLosses()).isEqualTo(1);
		assertThat(rating.getDraws()).isEqualTo(1);
		assertThat(rating.getGamesPlayed()).isEqualTo(3);
	}

	@Test
	@DisplayName("A change that would take the rating below 0 is a bug: IllegalArgumentException, nothing changes")
	void recordGame_belowZero_throws() {
		// Arrange
		PlayerRating rating = new PlayerRating(7);

		// Act + Assert
		assertThatThrownBy(() -> rating.recordGame(BattleOutcome.LOSS, -1001))
			.isInstanceOf(IllegalArgumentException.class);
		assertThat(rating.getRating()).isEqualTo(1000);
		assertThat(rating.getGamesPlayed()).isZero();
	}
}
