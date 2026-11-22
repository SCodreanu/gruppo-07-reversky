package it.downfall.reversky.api.battle.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.battle.entity.PlayerRating;
import jakarta.persistence.LockModeType;

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

	/**
	 * Legge il rating con {@code SELECT ... FOR UPDATE} (API-50): il salvataggio di una classificata blocca le due
	 * righe, con l'id minore per primo, prima di calcolare e applicare l'Elo.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from PlayerRating r where r.userId = :userId")
	Optional<PlayerRating> findByIdForUpdate(@Param("userId") Integer userId);
}
