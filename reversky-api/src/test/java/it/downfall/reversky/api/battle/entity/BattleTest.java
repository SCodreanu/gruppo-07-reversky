package it.downfall.reversky.api.battle.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.api.battle.client.SimulationRequest;

/**
 * {@link Battle}: le due fabbriche e la fotografia dell'Elo, che esiste solo per le classificate (API-49, API-50).
 */
class BattleTest {

	private static final BattleStats STATS = BattleStats.prepared(42L, new SimulationRequest.Rules("1.0", 30, 75),
		List.of(), List.of());

	@Test
	@DisplayName("A ranked battle has no invite and records both ratings before and their changes")
	void ranked_recordRatings_storesSnapshot() {
		// Arrange
		Battle battle = Battle.ranked(7, 3, 1, List.of(), List.of(), STATS, Instant.EPOCH);

		// Act
		battle.recordRatings(1000, 20, 1010, -20);

		// Assert
		assertThat(battle.getMode()).isEqualTo(BattleMode.RANKED);
		assertThat(battle.getInviteId()).isNull();
		assertThat(battle.getChallengerRatingBefore()).isEqualTo(1000);
		assertThat(battle.getChallengerRatingDelta()).isEqualTo(20);
		assertThat(battle.getOpponentRatingBefore()).isEqualTo(1010);
		assertThat(battle.getOpponentRatingDelta()).isEqualTo(-20);
	}

	@Test
	@DisplayName("A friendly battle never gets ratings")
	void friendly_recordRatings_throws() {
		// Arrange
		Battle battle = Battle.friendly(UUID.randomUUID(), 7, 3, 1, List.of(), List.of(), STATS, Instant.EPOCH);

		// Act + Assert
		assertThatThrownBy(() -> battle.recordRatings(1000, 20, 1010, -20))
			.isInstanceOf(IllegalStateException.class);
		assertThat(battle.getChallengerRatingDelta()).isNull();
	}
}
