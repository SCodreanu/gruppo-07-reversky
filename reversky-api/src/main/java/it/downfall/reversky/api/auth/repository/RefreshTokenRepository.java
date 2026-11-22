package it.downfall.reversky.api.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.auth.entity.RefreshToken;

/**
 * Accesso alla tabella {@code refresh_tokens}. Si cerca sempre per hash, mai per valore in chiaro.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

	@EntityGraph(attributePaths = {"user", "user.role"})
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Modifying
	@Query("DELETE FROM RefreshToken t WHERE t.user.id = :userId")
	int deleteAllByUserId(@Param("userId") Integer userId);

	@Modifying
	@Query("DELETE FROM RefreshToken t WHERE t.expiresAt <= :now")
	int deleteExpired(@Param("now") Instant now);

	// Le righe cancellate dicono chi ha vinto tra due rinnovi simultanei: 1 al primo, 0 al secondo.
	@Modifying
	@Query("DELETE FROM RefreshToken t WHERE t.tokenHash = :tokenHash")
	int deleteByTokenHash(@Param("tokenHash") String tokenHash);

}
