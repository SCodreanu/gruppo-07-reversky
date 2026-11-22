package it.downfall.reversky.api.battle.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.battle.dto.RankingResponse;
import it.downfall.reversky.api.battle.repository.PlayerRatingRepository;
import lombok.RequiredArgsConstructor;

/**
 * Rating Elo degli utenti (docs/sfide, "Elo — player_ratings"). Il calcolo delle variazioni è in {@link Elo}; qui
 * si leggono le righe e si crea quella di un utente nuovo.
 */
@Service
@RequiredArgsConstructor
public class RankingService {

	private final PlayerRatingRepository playerRatingRepository;

	/**
	 * Il mio rating e le statistiche classificate (GET /api/v1/rankings/me). Chi non è mai entrato in coda non ha
	 * ancora una riga: riceve i valori iniziali e la riga non si crea, perché una lettura non scrive.
	 */
	@Transactional(readOnly = true)
	public RankingResponse getMine(Integer userId) {
		return playerRatingRepository.findById(userId)
			.map(RankingResponse::from)
			.orElseGet(RankingResponse::initial);
	}

	/**
	 * Crea la riga del rating con i valori iniziali se l'utente non ce l'ha: si chiama al primo ingresso nella coda
	 * classificata (docs/sfide, "Classificata: matchmaking"), dentro la transazione del chiamante.
	 *
	 * @return {@code true} se la riga è stata creata ora
	 */
	@Transactional
	public boolean createIfMissing(Integer userId) {
		return playerRatingRepository.insertIfMissing(userId) == 1;
	}
}
