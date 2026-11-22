package it.downfall.reversky.api.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.downfall.reversky.api.user.entity.Role;

/**
 * Accesso alla tabella {@code roles}.
 */
public interface RoleRepository extends JpaRepository<Role, Integer> {

	// Trova un ruolo dal suo nome.
    Optional<Role> findByName(String name);
}
