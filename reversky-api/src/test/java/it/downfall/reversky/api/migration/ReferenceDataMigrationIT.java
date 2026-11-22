package it.downfall.reversky.api.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestUsers;

/**
 * Dati di riferimento della migrazione {@code V9} (API-15, #85): rarità, fazioni e rule set {@code 1.0} attivo.
 * <p>
 * Gli altri test aggiungono rarità (con {@code sorting_order} ≥ 1000) e fazioni con nomi univoci, e rimettono a posto
 * il rule set attivo e la matrice dei bonus dopo averli cambiati: per questo le query cercano le righe del seed per
 * ordine, id o versione, non contano tutte le righe.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class ReferenceDataMigrationIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// Non serve al test: stessa configurazione degli altri ControllerIT, così Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("The six catalog rarities have sorting order 1-6, the copy limits of Q-02 and the deck maximums of Q-03")
	void rarities_seededWithProposedLimits() {
		// Act
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
			"SELECT name, deck_limit, max_per_deck, sorting_order FROM rarities "
				+ "WHERE sorting_order BETWEEN 1 AND 6 ORDER BY sorting_order");

		// Assert
		assertThat(rows).containsExactly(
			rarity("Common", 3, null, 1),
			rarity("Uncommon", 2, null, 2),
			rarity("Rare", 2, 5, 3),
			rarity("Mythic", 1, 2, 4),
			rarity("Legendary", 1, 2, 5),
			rarity("Mutant", 1, 1, 6));
	}

	@Test
	@DisplayName("The four factions get ids 1-4 in the order of the engine examples, with an emblem and no description")
	void factions_seededInEngineOrder() {
		// Act
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
			"SELECT id, name, emblem_path, description FROM factions WHERE id BETWEEN 1 AND 4 ORDER BY id");

		// Assert: id come TestFactions dell'engine (WOUND 1, INVITATION 2, ARCHIVE 3, DREAM 4)
		assertThat(rows).containsExactly(
			faction(1, "Theory of the Wound", "factions/wound.svg"),
			faction(2, "Theory of the Invitation", "factions/invitation.svg"),
			faction(3, "Theory of the Archive", "factions/archive.svg"),
			faction(4, "Theory of the Dream", "factions/dream.svg"));
	}

	@Test
	@DisplayName("A faction created after the seed gets the next id: the identity sequence is in step with the seed")
	void factions_sequenceContinuesAfterSeed() {
		// Act: una fazione nuova, poi tolta per non lasciare dati
		Integer id = jdbcTemplate.queryForObject(
			"INSERT INTO factions (name, emblem_path) VALUES ('Seed sequence check', 'factions/x.svg') RETURNING id",
			Integer.class);
		jdbcTemplate.update("DELETE FROM factions WHERE id = ?", id);

		// Assert
		assertThat(id).isGreaterThan(4);
	}

	@Test
	@DisplayName("Rule set 1.0 is active with the 30-card deck and the proposed values, created by no user")
	void ruleSet_seededActive() {
		// Act
		Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM rule_sets WHERE version = '1.0'");

		// Assert
		assertThat(row)
			.containsEntry("is_active", true)
			.containsEntry("starting_life_points", 30)
			.containsEntry("max_rounds", 75)
			.containsEntry("deck_size", 30)
			.containsEntry("deck_creatures", 20)
			.containsEntry("deck_spells", 6)
			.containsEntry("deck_hidden", 2)
			.containsEntry("deck_items", 1)
			.containsEntry("deck_terrains", 1)
			.containsEntry("max_hidden_survival", 2)
			.containsEntry("max_hidden_damage", 1)
			.containsEntry("signup_bonus", 500)
			.containsEntry("win_reward", 30)
			.containsEntry("draw_reward", 10)
			.containsEntry("created_by_id", null);
	}

	@Test
	@DisplayName("On the seed GET /rules returns rule set 1.0 with no faction bonuses")
	void getRules_onSeed_returnsRuleSet10WithoutBonuses() throws Exception {
		mockMvc.perform(get("/api/v1/rules"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value("1.0"))
			.andExpect(jsonPath("$.startingLifePoints").value(30))
			.andExpect(jsonPath("$.deckSize").value(30))
			.andExpect(jsonPath("$.factionBonuses", hasSize(0)));
	}

	private static Map<String, Object> rarity(String name, int deckLimit, Integer maxPerDeck, int sortingOrder) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("name", name);
		row.put("deck_limit", deckLimit);
		row.put("max_per_deck", maxPerDeck);
		row.put("sorting_order", sortingOrder);
		return row;
	}

	private static Map<String, Object> faction(int id, String name, String emblemPath) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", id);
		row.put("name", name);
		row.put("emblem_path", emblemPath);
		row.put("description", null);
		return row;
	}
}
