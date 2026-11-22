package it.downfall.reversky.api.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import it.downfall.reversky.api.user.entity.User;

/**
 * Accesso alla tabella {@code users}.
 */
public interface UserRepository extends JpaRepository<User, Integer> {

	// Il login ha subito bisogno del ruolo per costruire il JWT.
	@EntityGraph(attributePaths = "role")
	Optional<User> findByEmail(String email);

	// Controlla che un utente esista dal suo username.
	boolean existsByUsername(String username);

	// Controlla che un utente esista dalla sua email.
	boolean existsByEmail(String email);
}
