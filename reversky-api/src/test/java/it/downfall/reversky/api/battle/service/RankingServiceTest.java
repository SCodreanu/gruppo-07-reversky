package it.downfall.reversky.api.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import it.downfall.reversky.api.battle.entity.BattleResult;
import it.downfall.reversky.api.battle.entity.PlayerRating;
import it.downfall.reversky.api.battle.repository.PlayerRatingRepository;
import it.downfall.reversky.api.battle.service.RankingService.RatingChanges;

/**
 * {@link RankingService#recordRankedGame} senza database (API-50): ordine dei blocchi, variazioni calcolate sui valori
 * di prima e applicate a entrambi. Il flusso con il database vero e i retry sovrapposti sono in
 * {@code RankedBattleFlowIT}.
 */
class RankingServiceTest {

	private final PlayerRatingRepository repository = mock(PlayerRatingRepository.class);
	private final RankingService rankingService = new RankingService(repository);

	@Test
	@DisplayName("The two rating rows are locked lower user id first, whoever is the challenger")
	void recordRankedGame_locksLowerIdFirst() {
		// Arrange: lo sfidante ha l'id maggiore
		when(repository.findByIdForUpdate(9)).thenReturn(Optional.of(new PlayerRating(9)));
		when(repository.findByIdForUpdate(4)).thenReturn(Optional.of(new PlayerRating(4)));

		// Act
		rankingService.recordRankedGame(9, 4, BattleResult.CHALLENGER_WIN);

		// Assert
		InOrder order = inOrder(repository);
		order.verify(repository).findByIdForUpdate(4);
		order.verify(repository).findByIdForUpdate(9);
	}

	@Test
	@DisplayName("Both changes come from the ratings before the battle and are applied to both players")
	void recordRankedGame_appliesBothChanges() {
		// Arrange: due giocatori nuovi, K = 40
		PlayerRating challenger = new PlayerRating(1);
		PlayerRating opponent = new PlayerRating(2);
		when(repository.findByIdForUpdate(1)).thenReturn(Optional.of(challenger));
		when(repository.findByIdForUpdate(2)).thenReturn(Optional.of(opponent));

		// Act
		RatingChanges changes = rankingService.recordRankedGame(1, 2, BattleResult.OPPONENT_WIN);

		// Assert
		assertThat(changes).isEqualTo(new RatingChanges(1000, -20, 1000, 20));
		assertThat(challenger.getRating()).isEqualTo(980);
		assertThat(challenger.getLosses()).isEqualTo(1);
		assertThat(opponent.getRating()).isEqualTo(1020);
		assertThat(opponent.getWins()).isEqualTo(1);
	}

	@Test
	@DisplayName("A ranked player without a rating row is a programming error, not a silent new rating")
	void recordRankedGame_missingRow_throws() {
		// Arrange
		when(repository.findByIdForUpdate(1)).thenReturn(Optional.of(new PlayerRating(1)));
		when(repository.findByIdForUpdate(2)).thenReturn(Optional.empty());

		// Act + Assert
		assertThatThrownBy(() -> rankingService.recordRankedGame(1, 2, BattleResult.DRAW))
			.isInstanceOf(IllegalStateException.class);
	}
}
