package it.downfall.reversky.api.ruleset.service;

import static it.downfall.reversky.api.support.TestRuleSets.request;
import static it.downfall.reversky.api.support.TestRuleSets.requestWithDeck;
import static it.downfall.reversky.api.support.TestRuleSets.uniqueVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.ruleset.dto.RuleSetPublicResponse;
import it.downfall.reversky.api.ruleset.dto.RuleSetRequest;
import it.downfall.reversky.api.ruleset.dto.RuleSetResponse;
import it.downfall.reversky.api.ruleset.dto.RuleSetSummaryResponse;
import it.downfall.reversky.api.ruleset.entity.RuleSet;
import it.downfall.reversky.api.ruleset.exception.NoActiveRuleSetException;
import it.downfall.reversky.api.ruleset.exception.RuleSetInUseException;
import it.downfall.reversky.api.ruleset.exception.RuleSetNotFoundException;
import it.downfall.reversky.api.ruleset.exception.RuleSetRulesViolatedException;
import it.downfall.reversky.api.ruleset.exception.RuleSetVersionAlreadyUsedException;
import it.downfall.reversky.api.ruleset.repository.RuleSetRepository;
import it.downfall.reversky.api.support.TestRuleSets;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link RuleSetService} su un PostgreSQL vero (docs/backoffice, "Regole di gioco").
 * <p>
 * Come AuthServiceIT la classe non è {@code @Transactional}: ogni chiamata fa il commit come in
 * produzione, quindi l'indice {@code uq_rule_sets_one_active} viene davvero controllato. Il rule set
 * attivo prima del test si rimette attivo dopo (vedi {@link TestRuleSets}).
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class RuleSetServiceIT {

	@Autowired
	private RuleSetService ruleSetService;

	@Autowired
	private RuleSetRepository ruleSetRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TestUsers testUsers;

	// Non serve al test: stessa configurazione degli altri IT, così Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	private TestRuleSets testRuleSets;

	@BeforeEach
	void setUp() {
		testRuleSets = new TestRuleSets(ruleSetService, ruleSetRepository, jdbcTemplate, testUsers);
		testRuleSets.rememberActive();
	}

	@AfterEach
	void tearDown() {
		testRuleSets.restoreActive();
	}

	// --- create ---

	@Test
	@DisplayName("A valid request creates an inactive rule set with every value of the request")
	void create_validRequest_savesInactiveRuleSet() {
		// Arrange
		RuleSetRequest request = request(uniqueVersion());

		// Act
		RuleSetResponse response = ruleSetService.create(request, null);

		// Assert
		assertThat(response)
			.usingRecursiveComparison()
			.ignoringFields("id", "isActive", "createdAt", "createdById")
			.isEqualTo(request);
		assertThat(response.isActive()).isFalse();
		assertThat(response.createdById()).isNull();
		assertThat(response.createdAt()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
		assertThat(ruleSetRepository.findById(response.id())).isPresent();
	}

	@Test
	@DisplayName("Creating with a version already in use is rejected")
	void create_versionAlreadyUsed_throwsVersionAlreadyUsed() {
		// Arrange
		RuleSetResponse existing = testRuleSets.create();

		// Act + Assert
		assertThatThrownBy(() -> ruleSetService.create(request(existing.version()), null))
			.isInstanceOf(RuleSetVersionAlreadyUsedException.class);
	}

	@Test
	@DisplayName("Deck parts that do not add up to the deck size are rejected, and nothing is saved")
	void create_deckPartsMismatch_throwsRulesViolated() {
		// Arrange: 20 + 6 + 2 + 1 + 2 = 31
		RuleSetRequest request = requestWithDeck(uniqueVersion(), 30, 20, 6, 2, 1, 2);

		// Act + Assert
		assertThatThrownBy(() -> ruleSetService.create(request, null))
			.isInstanceOf(RuleSetRulesViolatedException.class)
			.hasMessageContaining("(31)");
		assertThat(ruleSetRepository.existsByVersion(request.version())).isFalse();
	}

	@Test
	@DisplayName("Wrong deck parts are reported before a version already in use")
	void create_deckPartsMismatchAndVersionUsed_throwsRulesViolated() {
		// Arrange
		RuleSetResponse existing = testRuleSets.create();
		RuleSetRequest request = requestWithDeck(existing.version(), 30, 20, 6, 2, 1, 2);

		// Act + Assert
		assertThatThrownBy(() -> ruleSetService.create(request, null))
			.isInstanceOf(RuleSetRulesViolatedException.class);
	}

	// --- update ---

	@Test
	@DisplayName("A rule set never used by a battle can be updated")
	void update_unusedRuleSet_savesNewValues() {
		// Arrange
		RuleSetResponse created = testRuleSets.create();
		RuleSetRequest request = new RuleSetRequest(uniqueVersion(), 25, 60, 32, 20, 7, 2, 2, 1, 2, 1, 400, 40, 15);

		// Act
		RuleSetResponse response = ruleSetService.update(created.id(), request);

		// Assert
		assertThat(response)
			.usingRecursiveComparison()
			.ignoringFields("id", "isActive", "createdAt", "createdById")
			.isEqualTo(request);
		RuleSet saved = ruleSetRepository.findById(created.id()).orElseThrow();
		assertThat(saved.getVersion()).isEqualTo(request.version());
		assertThat(saved.getStartingLifePoints()).isEqualTo(25);
		// PostgreSQL salva i microsecondi, Instant ne può avere di più: si confronta con una tolleranza
		assertThat(saved.getCreatedAt()).isCloseTo(created.createdAt(), within(1, ChronoUnit.MILLIS));
	}

	@Test
	@DisplayName("A rule set can keep its own version when updated")
	void update_sameVersion_isAccepted() {
		// Arrange
		RuleSetResponse created = testRuleSets.create();

		// Act
		RuleSetResponse response = ruleSetService.update(created.id(), request(created.version()));

		// Assert
		assertThat(response.version()).isEqualTo(created.version());
	}

	@Test
	@DisplayName("Updating a rule set that does not exist is rejected")
	void update_unknownId_throwsNotFound() {
		// Act + Assert
		assertThatThrownBy(() -> ruleSetService.update(Integer.MAX_VALUE, request(uniqueVersion())))
			.isInstanceOf(RuleSetNotFoundException.class);
	}

	@Test
	@DisplayName("A rule set already used by a battle cannot be updated, and stays unchanged")
	void update_usedByBattle_throwsInUse() {
		// Arrange
		RuleSetResponse created = testRuleSets.create();
		testRuleSets.markUsedByBattle(created.id());

		// Act + Assert
		assertThatThrownBy(() -> ruleSetService.update(created.id(), request(uniqueVersion())))
			.isInstanceOf(RuleSetInUseException.class);
		assertThat(ruleSetRepository.findById(created.id()).orElseThrow().getVersion()).isEqualTo(created.version());
	}

	@Test
	@DisplayName("Updating to the version of another rule set is rejected")
	void update_versionOfAnother_throwsVersionAlreadyUsed() {
		// Arrange
		RuleSetResponse first = testRuleSets.create();
		RuleSetResponse second = testRuleSets.create();

		// Act + Assert
		assertThatThrownBy(() -> ruleSetService.update(second.id(), request(first.version())))
			.isInstanceOf(RuleSetVersionAlreadyUsedException.class);
	}

	@Test
	@DisplayName("Updating with deck parts that do not add up to the deck size is rejected")
	void update_deckPartsMismatch_throwsRulesViolated() {
		// Arrange
		RuleSetResponse created = testRuleSets.create();

		// Act + Assert
		assertThatThrownBy(() -> ruleSetService.update(created.id(), requestWithDeck(uniqueVersion(), 30, 19, 6, 2, 1, 1)))
			.isInstanceOf(RuleSetRulesViolatedException.class);
	}

	// --- activate ---

	@Test
	@DisplayName("Activating a rule set deactivates the previous one: only one stays active")
	void activate_anotherRuleSet_deactivatesThePreviousOne() {
		// Arrange
		RuleSetResponse first = testRuleSets.create();
		RuleSetResponse second = testRuleSets.create();
		ruleSetService.activate(first.id());

		// Act
		RuleSetResponse response = ruleSetService.activate(second.id());

		// Assert
		assertThat(response.isActive()).isTrue();
		assertThat(ruleSetRepository.findById(first.id()).orElseThrow().isActive()).isFalse();
		assertThat(ruleSetRepository.findByIsActiveTrue()).map(RuleSet::getId).contains(second.id());
	}

	@Test
	@DisplayName("Activating the rule set already active changes nothing")
	void activate_alreadyActive_isIdempotent() {
		// Arrange
		RuleSetResponse created = testRuleSets.create();
		ruleSetService.activate(created.id());

		// Act
		RuleSetResponse response = ruleSetService.activate(created.id());

		// Assert
		assertThat(response.isActive()).isTrue();
		assertThat(ruleSetRepository.findByIsActiveTrue()).map(RuleSet::getId).contains(created.id());
	}

	@Test
	@DisplayName("Activating a rule set when none is active works")
	void activate_noneActive_activatesIt() {
		// Arrange
		RuleSetResponse created = testRuleSets.create();
		jdbcTemplate.update("UPDATE rule_sets SET is_active = false WHERE is_active");

		// Act
		ruleSetService.activate(created.id());

		// Assert
		assertThat(ruleSetRepository.findByIsActiveTrue()).map(RuleSet::getId).contains(created.id());
	}

	@Test
	@DisplayName("Activating a rule set that does not exist is rejected")
	void activate_unknownId_throwsNotFound() {
		// Act + Assert
		assertThatThrownBy(() -> ruleSetService.activate(Integer.MAX_VALUE))
			.isInstanceOf(RuleSetNotFoundException.class);
	}

	// --- findActive ---

	@Test
	@DisplayName("Without an active rule set the active rules are reported as missing")
	void findActive_noneActive_throwsNoActive() {
		// Arrange
		jdbcTemplate.update("UPDATE rule_sets SET is_active = false WHERE is_active");

		// Act + Assert
		assertThatThrownBy(() -> ruleSetService.findActive())
			.isInstanceOf(NoActiveRuleSetException.class);
	}

	@Test
	@DisplayName("The active rules are those of the active rule set")
	void findActive_withActive_returnsIt() {
		// Arrange
		RuleSetResponse created = testRuleSets.create();
		ruleSetService.activate(created.id());

		// Act
		RuleSetPublicResponse response = ruleSetService.findActive();

		// Assert: factionBonuses è un campo nuovo della response (matrice di bonus); si ignora
		// esplicitamente perché RuleSetRequest non lo ha e non ha senso confrontarlo qui
		assertThat(response)
			.usingRecursiveComparison()
			.ignoringFields("factionBonuses")
			.isEqualTo(request(created.version()));
	}

	// --- findAll ---

	@Test
	@DisplayName("The admin list has every version, the most recent first")
	void findAll_twoRuleSets_mostRecentFirst() {
		// Arrange
		RuleSetResponse first = testRuleSets.create();
		RuleSetResponse second = testRuleSets.create();

		// Act
		List<Integer> ids = ruleSetService.findAll().stream().map(RuleSetSummaryResponse::id).toList();

		// Assert
		assertThat(ids).containsSubsequence(second.id(), first.id());
	}
}