package it.downfall.reversky.api.support;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import it.downfall.reversky.api.user.entity.User;

/**
 * Legge le notifiche di un utente per i test dei collegamenti (API-67, API-68, API-69, docs/test, "Dati di test").
 * <p>
 * Come {@link TestFriends} non è un {@code @TestComponent}: si crea con {@code new TestNotifications(jdbcTemplate)}.
 */
public class TestNotifications {

	private final JdbcTemplate jdbcTemplate;

	public TestNotifications(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/** I tipi delle notifiche dell'utente, dalla più vecchia. */
	public List<String> types(User user) {
		return jdbcTemplate.queryForList("SELECT type FROM notifications WHERE user_id = ? ORDER BY id", String.class,
			user.getId());
	}

	/** Il payload JSON dell'ultima notifica di quel tipo dell'utente, così com'è salvato. */
	public String lastPayload(User user, String type) {
		return jdbcTemplate.queryForObject("SELECT payload::text FROM notifications WHERE user_id = ? AND type = ? "
			+ "ORDER BY id DESC LIMIT 1", String.class, user.getId(), type);
	}
}
