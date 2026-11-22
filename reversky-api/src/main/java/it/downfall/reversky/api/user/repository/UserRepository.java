package it.downfall.reversky.api.user.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import it.downfall.reversky.api.user.entity.User;

/**
 * Accesso alla tabella {@code users}.
 */
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

	// GET /admin/users mostra il ruolo di ogni utente: si carica con la stessa query, senza N+1
	@Override
	@EntityGraph(attributePaths = "role")
	Page<User> findAll(Specification<User> specification, Pageable pageable);

	// Il login ha subito bisogno del ruolo per costruire il JWT.
	@EntityGraph(attributePaths = "role")
	Optional<User> findByEmail(String email);

	// Usato dalle amicizie: POST /friends/requests riceve lo username, non l'id.
	Optional<User> findByUsername(String username);

	// Controlla che un utente esista dal suo username.
	boolean existsByUsername(String username);

	// Controlla che un utente esista dalla sua email.
	boolean existsByEmail(String email);
}