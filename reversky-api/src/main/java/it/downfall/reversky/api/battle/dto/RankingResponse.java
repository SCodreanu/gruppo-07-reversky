package it.downfall.reversky.api.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.battle.entity.PlayerRating;

/**
 * Il mio rating Elo e le statistiche classificate (GET /api/v1/rankings/me, docs/sfide). Non c'è una classifica
 * pubblica né una posizione (Q-10).
 *
 * @param rating      rating attuale
 * @param peakRating  rating più alto raggiunto
 * @param gamesPlayed partite classificate giocate
 */
public record RankingResponse(

	@Schema(example = "1013")
	int rating,

	@Schema(example = "1040")
	int peakRating,

	@Schema(example = "12")
	int gamesPlayed,

	@Schema(example = "7")
	int wins,

	@Schema(example = "4")
	int losses,

	@Schema(example = "1")
	int draws
) {

	public static RankingResponse from(PlayerRating rating) {
		return new RankingResponse(rating.getRating(), rating.getPeakRating(), rating.getGamesPlayed(),
			rating.getWins(), rating.getLosses(), rating.getDraws());
	}

	/** Chi non è mai entrato in coda classificata: rating iniziale e nessuna partita. */
	public static RankingResponse initial() {
		return new RankingResponse(PlayerRating.INITIAL_RATING, PlayerRating.INITIAL_RATING, 0, 0, 0, 0);
	}
}
