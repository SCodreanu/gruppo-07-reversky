package it.downfall.reversky.api.support;

import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.service.UserService;
import lombok.RequiredArgsConstructor;

/**
 * Crea e modifica utenti di prova sul database dei test (docs/test, "Dati di test").
 * <p>
 * Si aggiunge al test con {@code @Import({TestcontainersConfiguration.class, TestUsers.class})}.
 * Ogni utente ha username ed email casuali, quindi i test non si influenzano tra loro.
 */
@TestComponent
@RequiredArgsConstructor
public class TestUsers {

	/** Password in chiaro di tutti gli utenti creati da {@link #create()}. */
	public static final String PASSWORD = "CorrectPassword1";

	private final UserService userService;
	private final PasswordEncoder passwordEncoder;
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Crea un utente USER abilitato, con email non verificata e password {@link #PASSWORD}.
	 */
	public User create() {
		String suffix = uniqueSuffix();

		return userService.createUser("user_" + suffix, suffix + "@example.com", passwordEncoder.encode(PASSWORD), Instant.now());
	}

	// SQL diretto: User non ha ancora un metodo per disabilitare, arriverà con il backoffice (#73)
	public void disable(User user) {
		jdbcTemplate.update("UPDATE users SET is_enabled = false WHERE id = ?", user.getId());
	}

	public void markEmailVerified(User user) {
		jdbcTemplate.update("UPDATE users SET email_verified_at = now() WHERE id = ?", user.getId());
	}

	// Va bene solo per utenti senza righe collegate (token, inventario...): non c'è ON DELETE CASCADE
	public void delete(User user) {
		jdbcTemplate.update("DELETE FROM users WHERE id = ?", user.getId());
	}

	/**
	 * 8 caratteri casuali, per username ed email che non si ripetono tra un test e l'altro.
	 */
	public static String uniqueSuffix() {
		return UUID.randomUUID().toString().substring(0, 8);
	}
}
