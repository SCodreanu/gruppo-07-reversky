package it.downfall.reversky.api.ruleset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.ruleset.dto.RuleSetCreateRequest;
import it.downfall.reversky.api.ruleset.dto.RuleSetPublicResponse;
import it.downfall.reversky.api.ruleset.dto.RuleSetResponse;
import it.downfall.reversky.api.ruleset.dto.RuleSetSummaryResponse;
import it.downfall.reversky.api.ruleset.dto.RuleSetUpdateRequest;
import it.downfall.reversky.api.ruleset.exception.NoActiveRuleSetException;
import it.downfall.reversky.api.ruleset.exception.RuleSetInUseException;
import it.downfall.reversky.api.ruleset.exception.RuleSetNotFoundException;
import it.downfall.reversky.api.ruleset.exception.RuleSetRulesViolatedException;
import it.downfall.reversky.api.ruleset.exception.RuleSetVersionAlreadyUsedException;
import it.downfall.reversky.api.ruleset.repository.RuleSetRepository;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * {@link RuleSetService} su un PostgreSQL vero (docs/backoffice, "Regole di gioco").
 * <p>
 * I rule set restano nel database tra un test e l'altro: ogni test usa una versione casuale.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class RuleSetServiceIT {

	@Autowired private RuleSetService ruleSetService;
	@Autowired private RuleSetRepository ruleSetRepository;
	@Autowired private JdbcTemplate jdbcTemplate;
	@Autowired private TestUsers testUsers;

	@MockitoBean private EmailSender emailSender;

	// --- create ---

	@Test
	@DisplayName("A valid request creates the rule set, inactive and with all fields")
	void create_validRequest_returnsSavedRuleSet() {
		String version = uniqueVersion();
		RuleSetCreateRequest request = createRequest(version);

		RuleSetResponse response = ruleSetService.create(request, null);

		assertThat(response.id()).isNotNull();
		assertThat(response.version()).isEqualTo(version);
		assertThat(response.isActive()).isFalse();
		assertThat(response.startingLifePoints()).isEqualTo(20);
		assertThat(response.maxRounds()).isEqualTo(10);
		assertThat(response.deckSize()).isEqualTo(30);
		assertThat(response.deckCreatures()).isEqualTo(15);
		assertThat(response.deckSpells()).isEqualTo(5);
		assertThat(response.deckHidden()).isEqualTo(4);
		assertThat(response.deckItems()).isEqualTo(3);
		assertThat(response.deckTerrains()).isEqualTo(3);
		assertThat(response.maxHiddenSurvival()).isEqualTo(2);
		assertThat(response.maxHiddenDamage()).isEqualTo(3);
		assertThat(response.signupBonus()).isEqualTo(100);
		assertThat(response.winReward()).isEqualTo(50);
		assertThat(response.drawReward()).isEqualTo(20);
		assertThat(response.createdAt()).isNotNull();
		assertThat(response.createdById()).isNull();
		assertThat(ruleSetRepository.findById(response.id())).isPresent();
	}

	@Test
	@DisplayName("A version already used is rejected")
	void create_versionAlreadyUsed_throwsVersionAlreadyUsed() {
		RuleSetResponse existing = ruleSetService.create(createRequest(uniqueVersion()), null);

		assertThatThrownBy(() -> ruleSetService.create(createRequest(existing.version()), null))
			.isInstanceOf(RuleSetVersionAlreadyUsedException.class);
	}

	@Test
	@DisplayName("Deck parts that do not sum to deckSize are rejected")
	void create_deckPartsMismatch_throwsRulesViolated() {
		RuleSetCreateRequest bad = new RuleSetCreateRequest(
			uniqueVersion(), 20, 10,
			30, 15, 5, 4, 3, 4, // somma 31, non 30
			2, 3, 100, 50, 20);

		assertThatThrownBy(() -> ruleSetService.create(bad, null))
			.isInstanceOf(RuleSetRulesViolatedException.class);
	}

	// --- update ---

	@Test
	@DisplayName("A rule set never used can be updated")
	void update_unusedRuleSet_changesFields() {
		RuleSetResponse created = ruleSetService.create(createRequest(uniqueVersion()), null);
		String newVersion = uniqueVersion();

		RuleSetUpdateRequest update = new RuleSetUpdateRequest(
			newVersion, 25, 15,
			32, 15, 6, 4, 3, 4,
			2, 3, 120, 60, 25);

		RuleSetResponse response = ruleSetService.update(created.id(), update);

		assertThat(response.version()).isEqualTo(newVersion);
		assertThat(response.startingLifePoints()).isEqualTo(25);
		assertThat(response.maxRounds()).isEqualTo(15);
		assertThat(response.deckSize()).isEqualTo(32);
		assertThat(ruleSetRepository.findById(created.id()).orElseThrow().getStartingLifePoints()).isEqualTo(25);
	}

	@Test
	@DisplayName("Updating an unknown id is rejected")
	void update_unknownId_throwsNotFound() {
		assertThatThrownBy(() -> ruleSetService.update(Integer.MAX_VALUE, updateRequest(uniqueVersion())))
			.isInstanceOf(RuleSetNotFoundException.class);
	}

	@Test
	@DisplayName("A rule set already used by a battle cannot be modified")
	void update_usedByBattle_throwsInUse() {
		RuleSetResponse created = ruleSetService.create(createRequest(uniqueVersion()), null);
		insertRankedBattle(created.id());

		assertThatThrownBy(() -> ruleSetService.update(created.id(), updateRequest(uniqueVersion())))
			.isInstanceOf(RuleSetInUseException.class);
	}

	@Test
	@DisplayName("Updating to a version already used by another rule set is rejected")
	void update_versionOfAnother_throwsVersionAlreadyUsed() {
		RuleSetResponse first = ruleSetService.create(createRequest(uniqueVersion()), null);
		RuleSetResponse second = ruleSetService.create(createRequest(uniqueVersion()), null);

		assertThatThrownBy(() -> ruleSetService.update(second.id(), updateRequest(first.version())))
			.isInstanceOf(RuleSetVersionAlreadyUsedException.class);
	}

	// --- activate ---

	@Test
	@DisplayName("Activating a rule set deactivates the previous one: only one is active")
	void activate_twoRuleSets_onlyOneIsActive() {
		RuleSetResponse first = ruleSetService.create(createRequest(uniqueVersion()), null);
		RuleSetResponse second = ruleSetService.create(createRequest(uniqueVersion()), null);

		ruleSetService.activate(first.id());
		ruleSetService.activate(second.id());

		assertThat(ruleSetRepository.findById(first.id()).orElseThrow().getIsActive()).isFalse();
		assertThat(ruleSetRepository.findById(second.id()).orElseThrow().getIsActive()).isTrue();
		assertThat(ruleSetRepository.findByIsActiveTrue().map(RuleSetSummaryResponse::from))
			.map(RuleSetSummaryResponse::id)
			.contains(second.id());
	}

	@Test
	@DisplayName("Activating an already active rule set is idempotent")
	void activate_alreadyActive_isIdempotent() {
		RuleSetResponse created = ruleSetService.create(createRequest(uniqueVersion()), null);
		ruleSetService.activate(created.id());

		RuleSetResponse again = ruleSetService.activate(created.id());

		assertThat(again.isActive()).isTrue();
		assertThat(ruleSetRepository.findAll().stream().filter(r -> r.getIsActive()).count()).isEqualTo(1);
	}

	@Test
	@DisplayName("Activating an unknown id is rejected")
	void activate_unknownId_throwsNotFound() {
		assertThatThrownBy(() -> ruleSetService.activate(Integer.MAX_VALUE))
			.isInstanceOf(RuleSetNotFoundException.class);
	}

	// --- findActive ---

	@Test
	@DisplayName("GET /rules with no active rule set is reported as missing")
	void findActive_noActive_throwsNoActive() {
		jdbcTemplate.update("UPDATE rule_sets SET is_active = false WHERE is_active = true");

		assertThatThrownBy(() -> ruleSetService.findActive())
			.isInstanceOf(NoActiveRuleSetException.class);
	}

	@Test
	@DisplayName("GET /rules returns the active rule set")
	void findActive_withActive_returnsIt() {
		RuleSetResponse active = ruleSetService.create(createRequest(uniqueVersion()), null);
		ruleSetService.activate(active.id());

		RuleSetPublicResponse response = ruleSetService.findActive();

		assertThat(response.version()).isEqualTo(active.version());
		assertThat(response.deckCreatures()).isEqualTo(15);
	}

	// --- findAll ---

	@Test
	@DisplayName("GET /admin/rule-sets returns every version, most recent first")
	void findAll_returnsEveryVersionOrderedByIdDesc() {
		RuleSetResponse first = ruleSetService.create(createRequest(uniqueVersion()), null);
		RuleSetResponse second = ruleSetService.create(createRequest(uniqueVersion()), null);

		List<RuleSetSummaryResponse> all = ruleSetService.findAll();

		assertThat(all).extracting(RuleSetSummaryResponse::id)
			.contains(first.id(), second.id());
		int indexFirst = all.indexOf(all.stream().filter(r -> r.id().equals(first.id())).findFirst().orElseThrow());
		int indexSecond = all.indexOf(all.stream().filter(r -> r.id().equals(second.id())).findFirst().orElseThrow());
		assertThat(indexSecond).isLessThan(indexFirst);
	}

	// --- supporto ---

	private static String uniqueVersion() {
		return "v" + UUID.randomUUID().toString().substring(0, 8);
	}

	private static RuleSetCreateRequest createRequest(String version) {
		return new RuleSetCreateRequest(
			version, 20, 10,
			30, 15, 5, 4, 3, 3,
			2, 3, 100, 50, 20);
	}

	private static RuleSetUpdateRequest updateRequest(String version) {
		return new RuleSetUpdateRequest(
			version, 20, 10,
			30, 15, 5, 4, 3, 3,
			2, 3, 100, 50, 20);
	}

	private void insertRankedBattle(Integer ruleSetId) {
		User challenger = testUsers.create();
		User opponent = testUsers.create();
		jdbcTemplate.update(
			"INSERT INTO battles (id, mode, challenger_id, opponent_id, rule_set_id, "
				+ "challenger_deck, opponent_deck, battle_stats, rounds_log) "
				+ "VALUES (?, 'RANKED', ?, ?, ?, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb)",
			UUID.randomUUID(), challenger.getId(), opponent.getId(), ruleSetId);
	}
}