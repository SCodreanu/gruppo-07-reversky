package it.downfall.reversky.api.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.auth.dto.LoginRequest;
import it.downfall.reversky.api.auth.service.AuthService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.deck.dto.DeckSummaryResponse;
import it.downfall.reversky.api.deck.service.DeckService;
import it.downfall.reversky.api.inventory.service.InventoryService;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.service.UserService;

/**
 * Dati di prova del profilo dev (API-25, #95): {@code db/dev/R__dev_seed.sql}.
 * <p>
 * Il profilo dev cambia la configurazione, quindi Spring avvia un contesto a parte con il suo container: i dati di
 * prova non arrivano mai nel database degli altri test.
 */
@ActiveProfiles("dev")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DevSeedIT {

	private static final String PASSWORD = "Reversky2026";
	private static final String SEED = "db/dev/R__dev_seed.sql";

	@Autowired
	private AuthService authService;

	@Autowired
	private UserService userService;

	@Autowired
	private DeckService deckService;

	@Autowired
	private InventoryService inventoryService;

	@Autowired
	private RuleSetService ruleSetService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("admin, alice and bob log in with the dev password; only admin is ADMIN")
	void seedUsers_logInWithDevPassword() {
		// Act + Assert
		for (String email : List.of("admin@reversky.dev", "alice@reversky.dev", "bob@reversky.dev")) {
			assertThat(authService.login(new LoginRequest(email, PASSWORD)).accessToken()).as(email).isNotBlank();
		}
		assertThat(userService.findByEmail("admin@reversky.dev").orElseThrow().getRole().getName()).isEqualTo("ADMIN");
		assertThat(userService.findByEmail("alice@reversky.dev").orElseThrow().getRole().getName()).isEqualTo("USER");
		assertThat(userService.findByEmail("alice@reversky.dev").orElseThrow().isEmailVerified()).isTrue();
	}

	@Test
	@DisplayName("alice and bob start with the signup bonus and one valid 30-card deck each")
	void seedPlayers_haveCreditsAndValidDeck() {
		// Arrange
		int signupBonus = ruleSetService.getActiveRuleSet().getSignupBonus();

		for (String email : List.of("alice@reversky.dev", "bob@reversky.dev")) {
			User user = userService.findByEmail(email).orElseThrow();

			// Act
			List<DeckSummaryResponse> decks = deckService.findMine(user.getId());

			// Assert
			assertThat(inventoryService.getCredits(user.getId())).as(email).isEqualTo(signupBonus);
			assertThat(decks).as(email).hasSize(1);
			assertThat(decks.get(0).cardCount()).as(email).isEqualTo(30);
			assertThat(decks.get(0).valid()).as(email).isTrue();
		}
	}

	@Test
	@DisplayName("alice and bob are friends and own every active original card")
	void seedPlayers_areFriendsWithFullCollection() {
		// Act
		Integer friendships = jdbcTemplate.queryForObject("""
			SELECT count(*) FROM friendships f
			JOIN users a ON a.id = f.requester_id JOIN users b ON b.id = f.recipient_id
			WHERE f.status = 'ACCEPTED' AND a.email = 'alice@reversky.dev' AND b.email = 'bob@reversky.dev'""", Integer.class);
		Integer originals = jdbcTemplate.queryForObject(
			"SELECT count(*) FROM catalog_cards WHERE is_active AND variant_of_id IS NULL", Integer.class);
		Integer aliceCards = jdbcTemplate.queryForObject("""
			SELECT count(*) FROM collection_cards cc JOIN users u ON u.id = cc.user_id
			WHERE u.email = 'alice@reversky.dev'""", Integer.class);

		// Assert
		assertThat(friendships).isEqualTo(1);
		assertThat(aliceCards).isEqualTo(originals).isEqualTo(120);
	}

	@Test
	@DisplayName("Running the seed again creates no duplicates")
	void seed_rerun_createsNothingNew() throws Exception {
		// Arrange
		String counts = """
			SELECT (SELECT count(*) FROM users) || '/' || (SELECT count(*) FROM credit_transactions) || '/'
			    || (SELECT count(*) FROM collection_cards) || '/' || (SELECT sum(quantity) FROM collection_cards) || '/'
			    || (SELECT count(*) FROM decks) || '/' || (SELECT count(*) FROM deck_cards) || '/'
			    || (SELECT count(*) FROM friendships)""";
		String before = jdbcTemplate.queryForObject(counts, String.class);

		// Act
		jdbcTemplate.execute(new ClassPathResource(SEED).getContentAsString(StandardCharsets.UTF_8));

		// Assert
		assertThat(jdbcTemplate.queryForObject(counts, String.class)).isEqualTo(before);
	}
}
