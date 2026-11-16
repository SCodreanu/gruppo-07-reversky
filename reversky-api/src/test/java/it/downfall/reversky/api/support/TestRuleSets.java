package it.downfall.reversky.api.support;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

import it.downfall.reversky.api.ruleset.dto.RuleSetRequest;
import it.downfall.reversky.api.ruleset.dto.RuleSetResponse;
import it.downfall.reversky.api.ruleset.entity.RuleSet;
import it.downfall.reversky.api.ruleset.repository.RuleSetRepository;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.user.entity.User;

/**
 * Rule set di prova sul database dei test.
 * <p>
 * Come {@link TestCards} non è un {@code @TestComponent}: si crea nel test con
 * {@code new TestRuleSets(ruleSetService, ruleSetRepository, jdbcTemplate, testUsers)}, così
 * {@code @Import} resta uguale agli altri IT e Spring riusa lo stesso contesto.
 * <p>
 * Il rule set attivo è uno solo per tutto il database, e altre funzionalità lo leggono (crediti di
 * benvenuto, mazzi, sfide). Un test che attiva o disattiva rule set chiama {@link #rememberActive()}
 * prima e {@link #restoreActive()} dopo, così il seed (#85) resta attivo per gli altri test.
 */
public class TestRuleSets {

	private final RuleSetService ruleSetService;
	private final RuleSetRepository ruleSetRepository;
	private final JdbcTemplate jdbcTemplate;
	private final TestUsers testUsers;

	private Integer rememberedActiveId;

	public TestRuleSets(RuleSetService ruleSetService, RuleSetRepository ruleSetRepository, JdbcTemplate jdbcTemplate, TestUsers testUsers) {
		this.ruleSetService = ruleSetService;
		this.ruleSetRepository = ruleSetRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.testUsers = testUsers;
	}

	/**
	 * Versione univoca: i rule set non si cancellano e restano nel database tra un test e l'altro.
	 */
	public static String uniqueVersion() {
		return "test-" + TestUsers.uniqueSuffix();
	}

	/**
	 * Richiesta valida con i valori del rule set {@code 1.0} (docs/mazzi, docs/crediti, docs/decisioni).
	 */
	public static RuleSetRequest request(String version) {
		return new RuleSetRequest(version, 30, 75, 30, 20, 6, 2, 1, 1, 2, 1, 500, 30, 10);
	}

	/**
	 * Richiesta con una composizione del mazzo scelta dal test; gli altri valori sono quelli di {@link #request(String)}.
	 */
	public static RuleSetRequest requestWithDeck(String version, int deckSize, int creatures, int spells, int hidden, int items, int terrains) {
		return new RuleSetRequest(version, 30, 75, deckSize, creatures, spells, hidden, items, terrains, 2, 1, 500, 30, 10);
	}

	/**
	 * Il JSON che manderebbe il frontend per questa richiesta.
	 */
	public static String toJson(RuleSetRequest request) {
		return """
			{"version": "%s", "startingLifePoints": %s, "maxRounds": %s, "deckSize": %s, "deckCreatures": %s,
			"deckSpells": %s, "deckHidden": %s, "deckItems": %s, "deckTerrains": %s, "maxHiddenSurvival": %s,
			"maxHiddenDamage": %s, "signupBonus": %s, "winReward": %s, "drawReward": %s}
			""".formatted(request.version(), request.startingLifePoints(), request.maxRounds(), request.deckSize(),
				request.deckCreatures(), request.deckSpells(), request.deckHidden(), request.deckItems(),
				request.deckTerrains(), request.maxHiddenSurvival(), request.maxHiddenDamage(), request.signupBonus(),
				request.winReward(), request.drawReward());
	}

	/**
	 * Crea un rule set disattivato con i valori di {@link #request(String)} e una versione univoca.
	 */
	public RuleSetResponse create() {
		return ruleSetService.create(request(uniqueVersion()), null);
	}

	/**
	 * Ricorda il rule set attivo (o la sua assenza), da ripristinare con {@link #restoreActive()}.
	 */
	public void rememberActive() {
		rememberedActiveId = ruleSetRepository.findByIsActiveTrue()
			.map(RuleSet::getId)
			.orElse(null);
	}

	/**
	 * Rimette attivo il rule set ricordato da {@link #rememberActive()}, oppure nessuno se non c'era.
	 */
	public void restoreActive() {
		if (rememberedActiveId != null) {
			ruleSetService.activate(rememberedActiveId);
			return;
		}

		// SQL diretto: il service non ha un'operazione "nessun rule set attivo", e non deve averla
		jdbcTemplate.update("UPDATE rule_sets SET is_active = false WHERE is_active");
	}

	/**
	 * Segna il rule set come già usato inserendo una partita classificata che lo usa.
	 * <p>
	 * SQL diretto: l'entità Battle non esiste ancora. I JSON vuoti bastano ai vincoli della tabella.
	 */
	public void markUsedByBattle(Integer ruleSetId) {
		User challenger = testUsers.create();
		User opponent = testUsers.create();

		jdbcTemplate.update(
			"INSERT INTO battles (id, mode, challenger_id, opponent_id, rule_set_id, challenger_deck, opponent_deck, battle_stats, rounds_log) "
				+ "VALUES (?, 'RANKED', ?, ?, ?, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb)",
			UUID.randomUUID(), challenger.getId(), opponent.getId(), ruleSetId);
	}
}
