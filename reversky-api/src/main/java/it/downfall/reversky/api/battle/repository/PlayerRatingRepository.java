package it.downfall.reversky.api.battle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.battle.entity.PlayerRating;

/**
 * Rating Elo degli utenti ({@code player_ratings}).
 */
public interface PlayerRatingRepository extends JpaRepository<PlayerRating, Integer> {

	/**
	 * Crea la riga con i valori iniziali delle colonne se l'utente non ce l'ha ancora. Con {@code ON CONFLICT DO
	 * NOTHING} due ingressi in coda nello stesso momento non falliscono: il secondo non fa niente.
	 *
	 * @return 1 se la riga è stata creata, 0 se c'era già
	 */
	@Modifying
	@Query(value = "INSERT INTO player_ratings (user_id) VALUES (:userId) ON CONFLICT (user_id) DO NOTHING",
		nativeQuery = true)
	int insertIfMissing(@Param("userId") Integer userId);
}
