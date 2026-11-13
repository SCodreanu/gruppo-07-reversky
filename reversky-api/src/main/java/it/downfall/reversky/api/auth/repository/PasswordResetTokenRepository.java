package it.downfall.reversky.api.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.auth.entity.PasswordResetToken;

/**
 * Accesso alla tabella {@code password_reset_tokens}.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

	@EntityGraph(attributePaths = "user")
	Optional<PasswordResetToken> findByTokenHash(String tokenHash);

	@Modifying
	@Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
	int deleteAllByUserId(@Param("userId") Integer userId);

	@Modifying
	@Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt <= :now")
	int deleteExpired(@Param("now") Instant now);
}
