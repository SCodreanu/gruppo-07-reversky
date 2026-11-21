package it.downfall.reversky.api.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestUsers;

/**
 * Tabella {@code effects} e chiave esterna {@code fk_catalog_cards_effect} della migrazione {@code V13} (API-36, #106).
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class EffectsMigrationIT {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// Non serve al test: stessa configurazione degli altri IT, così Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("V13 inserts the 92 effects of the engine registry: 64 creatures, 12 Spells, 8 Hidden, 4 Terrains, 4 Items")
	void effects_seededFromEngineRegistry() {
		// Act
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
			"SELECT applies_to::text AS type, count(*) AS total FROM effects GROUP BY applies_to ORDER BY applies_to");

		// Assert
		assertThat(rows).containsExactly(
			Map.of("type", "CREATURE", "total", 64L),
			Map.of("type", "TERRAIN", "total", 4L),
			Map.of("type", "SPELL", "total", 12L),
			Map.of("type", "HIDDEN", "total", 8L),
			Map.of("type", "ITEM", "total", 4L));
	}

	@Test
	@DisplayName("Every effect of the initial catalog is in the effects table with the card's type")
	void initialCatalog_usesOnlySeededEffects() {
		// Act: le carte di V11 con un effetto che non trova la riga dello stesso tipo
		Integer orphans = jdbcTemplate.queryForObject("""
			SELECT count(*) FROM catalog_cards c
			WHERE c.effect_code IS NOT NULL
			  AND NOT EXISTS (SELECT 1 FROM effects e WHERE e.code = c.effect_code AND e.applies_to = c.type)
			""", Integer.class);

		// Assert
		assertThat(orphans).isZero();
	}

	@Test
	@DisplayName("The database rejects an effect code that is not in the effects table")
	void update_unknownEffect_isRejected() {
		// Arrange
		Integer spellId = cardWithEffect("VEIL_OF_ANCHORED");

		// Act + Assert
		assertThatThrownBy(() -> jdbcTemplate.update("UPDATE catalog_cards SET effect_code = 'NOT_IN_ENGINE' WHERE id = ?", spellId))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("fk_catalog_cards_effect");
	}

	@Test
	@DisplayName("The database rejects an effect code of another card type")
	void update_effectOfAnotherType_isRejected() {
		// Arrange: SPEARFINGER è un effetto di creatura, la carta è una Spell
		Integer spellId = cardWithEffect("VEIL_OF_ANCHORED");

		// Act + Assert
		assertThatThrownBy(() -> jdbcTemplate.update("UPDATE catalog_cards SET effect_code = 'SPEARFINGER' WHERE id = ?", spellId))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("fk_catalog_cards_effect");
	}

	// Una carta di V11 (originale) con quell'effetto
	private Integer cardWithEffect(String effectCode) {
		return jdbcTemplate.queryForObject(
			"SELECT id FROM catalog_cards WHERE effect_code = ? AND variant_of_id IS NULL ORDER BY id LIMIT 1",
			Integer.class, effectCode);
	}
}
