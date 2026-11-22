package it.downfall.reversky.api.battle.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.api.battle.dto.BattleOutcome;

/**
 * {@link Elo}: le variazioni di docs/sfide ("Elo — player_ratings") e i criteri di accettazione di API-46.
 */
class EloTest {

	// Oltre le prime 10 partite: K = 20
	private static final int ESTABLISHED = 10;

	@Test
	@DisplayName("1000 against 1100 with K = 20: if 1000 wins +13 and -13, if 1100 wins -7 and +7")
	void eloDelta_issueExamples() {
		// Act + Assert: vince lo sfavorito
		assertThat(Elo.eloDelta(1000, 1100, 1.0, ESTABLISHED)).isEqualTo(13);
		assertThat(Elo.eloDelta(1100, 1000, 0.0, ESTABLISHED)).isEqualTo(-13);
		// vince il favorito
		assertThat(Elo.eloDelta(1000, 1100, 0.0, ESTABLISHED)).isEqualTo(-7);
		assertThat(Elo.eloDelta(1100, 1000, 1.0, ESTABLISHED)).isEqualTo(7);
	}

	@Test
	@DisplayName("A draw between equal ratings changes nothing")
	void eloDelta_drawEqualRatings_zero() {
		assertThat(Elo.eloDelta(1000, 1000, 0.5, ESTABLISHED)).isZero();
		assertThat(Elo.eloDelta(1000, 1000, 0.5, 0)).isZero();
	}

	@Test
	@DisplayName("K is 40 for the first 10 ranked games and 20 from the 11th: a win between equals is +20, then +10")
	void eloDelta_kDependsOnGamesPlayedBefore() {
		assertThat(Elo.eloDelta(1000, 1000, 1.0, 0)).isEqualTo(20);
		assertThat(Elo.eloDelta(1000, 1000, 1.0, 9)).isEqualTo(20);
		assertThat(Elo.eloDelta(1000, 1000, 1.0, 10)).isEqualTo(10);
	}

	@Test
	@DisplayName("Players with different K get changes that are not equal and opposite")
	void eloDelta_differentK_notSymmetric() {
		// Act: un giocatore nuovo batte uno esperto con lo stesso rating
		int newcomer = Elo.eloDelta(1000, 1000, 1.0, 0);
		int veteran = Elo.eloDelta(1000, 1000, 0.0, 30);

		// Assert
		assertThat(newcomer).isEqualTo(20);
		assertThat(veteran).isEqualTo(-10);
	}

	@Test
	@DisplayName("The rating never goes below 0: a loss that would cost more than the rating takes it to 0")
	void eloDelta_neverBelowZero() {
		// Arrange: con K = 40 una sconfitta tra pari vale -20, ma il giocatore ha solo 10 punti
		assertThat(Elo.eloDelta(10, 10, 0.0, 0)).isEqualTo(-10);
		assertThat(Elo.eloDelta(0, 0, 0.0, 0)).isZero();
	}

	@Test
	@DisplayName("Score: win 1, draw 0.5, loss 0")
	void score_byOutcome() {
		assertThat(Elo.score(BattleOutcome.WIN)).isEqualTo(1.0);
		assertThat(Elo.score(BattleOutcome.DRAW)).isEqualTo(0.5);
		assertThat(Elo.score(BattleOutcome.LOSS)).isEqualTo(0.0);
	}
}
