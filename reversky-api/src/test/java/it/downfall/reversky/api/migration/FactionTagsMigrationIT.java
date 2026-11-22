package it.downfall.reversky.api.migration;

import static org.assertj.core.api.Assertions.assertThat;

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

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestUsers;

/**
 * Tag delle quattro fazioni della migrazione {@code V15} (API-13, #83): uno per fazione, collegato per nome.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class FactionTagsMigrationIT {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// Non serve al test: stessa configurazione degli altri ControllerIT, così Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("Each faction of V9 has its own tag: WOUND, INVITATION, ARCHIVE, DREAM")
	void factionTags_oneTagPerFaction() {
		// Act
		List<Map<String, Object>> links = jdbcTemplate.queryForList("""
				SELECT f.name AS faction, t.name AS tag
				FROM faction_tags ft
				JOIN factions f ON f.id = ft.faction_id
				JOIN tags t ON t.id = ft.tag_id
				WHERE t.name IN ('WOUND', 'INVITATION', 'ARCHIVE', 'DREAM')
				ORDER BY t.name
				""");

		// Assert
		assertThat(links).extracting(row -> row.get("faction") + " -> " + row.get("tag")).containsExactly(
			"Theory of the Archive -> ARCHIVE",
			"Theory of the Dream -> DREAM",
			"Theory of the Invitation -> INVITATION",
			"Theory of the Wound -> WOUND");
	}

	@Test
	@DisplayName("The four tags are linked to their faction only")
	void factionTags_eachTagOnOneFaction() {
		Integer links = jdbcTemplate.queryForObject("""
				SELECT count(*) FROM faction_tags ft JOIN tags t ON t.id = ft.tag_id
				WHERE t.name IN ('WOUND', 'INVITATION', 'ARCHIVE', 'DREAM')
				""", Integer.class);

		assertThat(links).isEqualTo(4);
	}
}
