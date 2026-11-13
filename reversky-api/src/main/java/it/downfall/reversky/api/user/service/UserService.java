package it.downfall.reversky.api.user.service;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.user.dto.UserResponse;
import it.downfall.reversky.api.user.entity.Role;
import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.exception.UserNotFoundException;
import it.downfall.reversky.api.user.repository.RoleRepository;
import it.downfall.reversky.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * Gestione degli utenti.
 * <p>
 * Gli altri package (es. auth) accedono agli utenti solo da qui, mai dai repository di questo
 * package (CodingRequirements §4). Le email vengono sempre normalizzate (trim + minuscolo) qui,
 * così chi chiama non deve ricordarsene.
 */
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

	/**
	 * @return true se lo username è già usato da un altro utente
	 */
	@Transactional(readOnly = true)
	public boolean isUsernameTaken(String username) {
		return userRepository.existsByUsername(username);
	}

	/**
	 * @return true se l'email (normalizzata) è già usata da un altro utente
	 */
	@Transactional(readOnly = true)
	public boolean isEmailTaken(String email) {
		return userRepository.existsByEmail(normalizeEmail(email));
	}

	/**
	 * Cerca un utente per email, con il ruolo già caricato.
	 * <p>
	 * Restituisce un Optional e non lancia eccezioni: il login trasforma l'assenza in
	 * "credenziali non valide", il reset password invece non deve dire niente.
	 *
	 * @param email email inserita dall'utente, anche con maiuscole o spazi
	 */
	@Transactional(readOnly = true)
	public Optional<User> findByEmail(String email) {
		return userRepository.findByEmail(normalizeEmail(email));
	}

	/**
	 * Restituisce un utente dal suo id (quello nel campo {@code sub} del JWT).
	 *
	 * @throws UserNotFoundException se l'utente non esiste
	 */
	@Transactional(readOnly = true)
	public User getById(Integer id) {
		return userRepository.findById(id)
			.orElseThrow(() -> new UserNotFoundException());
	}

	/**
	 * Crea un nuovo utente con ruolo USER, abilitato e con email non verificata.
	 *
	 * @param passwordHash password già cifrata con BCrypt
	 * @param acceptedAt   momento di accettazione di privacy e termini
	 * @return l'utente salvato, con l'id assegnato
	 * @throws IllegalStateException se il ruolo USER manca nel database (errore di migrazione, non dell'utente)
	 */
	@Transactional
	public User createUser(String username, String email, String passwordHash, Instant acceptedAt) {

		Role userRole = roleRepository.findByName(Role.USER)
			.orElseThrow(() -> new IllegalStateException("Role USER not found: check migration V2"));

		return userRepository.save(new User(userRole, username, normalizeEmail(email), passwordHash, acceptedAt));
	}

	/**
	 * Profilo dell'utente autenticato (GET /api/v1/users/me).
	 *
	 * @param id id dell'utente, dal campo {@code sub} del JWT
	 * @throws UserNotFoundException se l'utente non esiste più (token ancora valido di un account eliminato)
	 */
	@Transactional(readOnly = true)
	public UserResponse getProfile(Integer id) {
		return UserResponse.from(getById(id));
	}

	// "  Mario@Example.com " -> "mario@example.com": il vincolo UNIQUE di Postgres distingue le maiuscole
	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
