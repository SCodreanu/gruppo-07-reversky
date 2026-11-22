package it.downfall.reversky.api.battle.service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.battle.dto.BattleOutcome;
import it.downfall.reversky.api.battle.dto.RankingResponse;
import it.downfall.reversky.api.battle.entity.BattleResult;
import it.downfall.reversky.api.battle.entity.PlayerRating;
import it.downfall.reversky.api.battle.repository.PlayerRatingRepository;
import lombok.RequiredArgsConstructor;

/**
 * Rating Elo degli utenti (docs/sfide, "Elo — player_ratings"). Il calcolo delle variazioni è in {@link Elo}; qui
 * si leggono le righe, si crea quella di un utente nuovo e si applica l'esito di una classificata (API-50).
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

	/**
	 * Applica l'esito di una partita classificata ai rating dei due giocatori (docs/sfide, "Elo — player_ratings",
	 * API-50), dentro la transazione che salva il risultato. Blocca le due righe di {@code player_ratings} con l'id
	 * minore per primo, calcola le due variazioni con {@link Elo#eloDelta} sui rating e sulle partite giocate di
	 * adesso, cioè di prima della partita, e le applica con {@code PlayerRating.recordGame}.
	 * <p>
	 * Il chiamante lo usa una sola volta per partita: lo garantisce il blocco della partita in {@code BattleService},
	 * che non salva di nuovo una partita già completata.
	 *
	 * @param challengerId lo sfidante della partita
	 * @param opponentId   l'avversario della partita
	 * @param result       l'esito dell'engine
	 * @return i rating di prima e le variazioni, da scrivere nella partita
	 * @throws IllegalStateException se uno dei due non ha la riga del rating: si crea all'ingresso in coda, quindi un
	 *                               giocatore abbinato ce l'ha sempre
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public RatingChanges recordRankedGame(Integer challengerId, Integer opponentId, BattleResult result) {
		Map<Integer, PlayerRating> locked = lockRatings(challengerId, opponentId);
		PlayerRating challenger = locked.get(challengerId);
		PlayerRating opponent = locked.get(opponentId);
		BattleOutcome challengerOutcome = BattleOutcome.of(result, true);
		BattleOutcome opponentOutcome = BattleOutcome.of(result, false);

		// Entrambe le variazioni si calcolano sui valori di prima: si applicano solo dopo
		int challengerBefore = challenger.getRating();
		int opponentBefore = opponent.getRating();
		int challengerDelta = Elo.eloDelta(challengerBefore, opponentBefore, Elo.score(challengerOutcome),
			challenger.getGamesPlayed());
		int opponentDelta = Elo.eloDelta(opponentBefore, challengerBefore, Elo.score(opponentOutcome),
			opponent.getGamesPlayed());
		challenger.recordGame(challengerOutcome, challengerDelta);
		opponent.recordGame(opponentOutcome, opponentDelta);

		return new RatingChanges(challengerBefore, challengerDelta, opponentBefore, opponentDelta);
	}

	// L'id minore per primo: due salvataggi con giocatori in comune bloccano nello stesso ordine, senza deadlock
	private Map<Integer, PlayerRating> lockRatings(Integer firstId, Integer secondId) {
		Map<Integer, PlayerRating> locked = new HashMap<>();
		for (Integer userId : Stream.of(firstId, secondId).sorted().toList()) {
			locked.put(userId, playerRatingRepository.findByIdForUpdate(userId)
				.orElseThrow(() -> new IllegalStateException("Ranked player " + userId + " has no rating")));
		}
		return locked;
	}

	/**
	 * Rating dei due giocatori prima di una partita classificata e loro variazioni (colonne {@code *_rating_before} e
	 * {@code *_rating_delta} di {@code battles}).
	 */
	public record RatingChanges(int challengerBefore, int challengerDelta, int opponentBefore, int opponentDelta) {
	}
}
