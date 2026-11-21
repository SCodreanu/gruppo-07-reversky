package it.downfall.reversky.api.migration;

import static it.downfall.reversky.api.support.TestUsers.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.service.UserService;

/**
 * Migrazione {@code V10} (API-17, #87): inventario per gli utenti registrati prima che la registrazione lo creasse.
 * <p>
 * Su un database vuoto Flyway la esegue senza effetti, quindi il test crea un utente senza inventario (come prima di
 * API-17) e rilancia lo stesso SQL. Gli utenti di {@link TestUsers} hanno già l'inventario e non vengono toccati.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class MissingInventoriesMigrationIT {

	private static final String MIGRATION = "db/migration/V10__create_missing_inventories.sql";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private UserService userService;

	@Autowired
	private RuleSetService ruleSetService;

	@Autowired
	private TestUsers testUsers;

	// Non serve al test: stessa configurazione degli altri IT, così Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("A user without an inventory gets the signup bonus and one SIGNUP row; others are untouched")
	void migration_userWithoutInventory_getsSignupBonus() throws Exception {
		// Arrange: UserService.createUser non crea l'inventario, lo fa AuthService.register
		String suffix = uniqueSuffix();
		User legacy = userService.createUser("user_" + suffix, suffix + "@example.com", "hash", Instant.now());
		User current = testUsers.create();
		int signupBonus = ruleSetService.getActiveRuleSet().getSignupBonus();

		// Act: due volte, per verificare che non crei doppioni
		runMigration();
		runMigration();

		// Assert
		assertThat(creditsOf(legacy)).isEqualTo(signupBonus);
		assertThat(transactionsOf(legacy)).containsExactly(Map.of("amount", signupBonus, "reason", "SIGNUP"));
		assertThat(transactionsOf(current)).hasSize(1);
	}

	private void runMigration() throws Exception {
		jdbcTemplate.execute(new ClassPathResource(MIGRATION).getContentAsString(StandardCharsets.UTF_8));
	}

	private Integer creditsOf(User user) {
		return jdbcTemplate.queryForObject("SELECT credits FROM inventories WHERE user_id = ?", Integer.class, user.getId());
	}

	private List<Map<String, Object>> transactionsOf(User user) {
		return jdbcTemplate.queryForList(
			"SELECT amount, reason::text AS reason FROM credit_transactions WHERE user_id = ? ORDER BY id", user.getId());
	}
}
