package it.downfall.reversky.api.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.entity.CardSubtype;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestUsers;

/**
 * Catalogo iniziale della migrazione {@code V11} (API-55, #125): 120 carte da catalogo-finale.csv.
 * <p>
 * Gli altri test aggiungono carte con nomi univoci e immagine {@code /images/...}: le query cercano solo le carte del
 * seed, riconosciute dall'immagine segnaposto {@code cards/...}.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class InitialCatalogMigrationIT {

	private static final String SEED = " FROM catalog_cards c WHERE c.image_path LIKE 'cards/%' ";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// Non serve al test: stessa configurazione degli altri IT, così Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("The catalog has 92 creatures, 12 Spells, 8 Hidden, 4 Terrains and 4 Items, all active originals")
	void catalog_countsByType() {
		// Act
		Map<String, Integer> counts = new java.util.TreeMap<>();
		jdbcTemplate.query("SELECT c.type::text AS type, count(*) AS total" + SEED
				+ "AND c.is_active AND c.variant_of_id IS NULL GROUP BY c.type",
			rs -> {
				counts.put(rs.getString("type"), rs.getInt("total"));
			});

		// Assert
		assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of(
			"CREATURE", 92, "SPELL", 12, "HIDDEN", 8, "TERRAIN", 4, "ITEM", 4));
	}

	@Test
	@DisplayName("Every card respects the rarity and subtype rules of its type")
	void catalog_rarityAndSubtypeMatchType() {
		// Act
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
			"SELECT c.name, c.type::text AS type, c.subtype::text AS subtype, r.sorting_order" + SEED.replace("WHERE",
				"JOIN rarities r ON r.id = c.rarity_id WHERE"));

		// Assert
		assertThat(rows).hasSize(120).allSatisfy(row -> {
			CardType type = CardType.valueOf((String) row.get("type"));
			Rarity rarity = new Rarity("Seed", 1, null, (Integer) row.get("sorting_order"));
			CardSubtype subtype = row.get("subtype") == null ? null : CardSubtype.valueOf((String) row.get("subtype"));
			assertThat(type.acceptsRarity(rarity)).as("rarity of %s", row.get("name")).isTrue();
			assertThat(type.acceptsSubtype(subtype)).as("subtype of %s", row.get("name")).isTrue();
		});
	}

	@Test
	@DisplayName("Each faction has its creatures, one Terrain and one Item; the two Mutants have a second faction")
	void catalog_factions() {
		// Act
		List<Map<String, Object>> perFaction = jdbcTemplate.queryForList("""
			SELECT f.name, count(*) FILTER (WHERE c.type = 'CREATURE') AS creatures,
			       count(*) FILTER (WHERE c.type = 'TERRAIN') AS terrains, count(*) FILTER (WHERE c.type = 'ITEM') AS items
			FROM catalog_cards c JOIN factions f ON f.id = c.faction_id
			WHERE c.image_path LIKE 'cards/%' GROUP BY f.name ORDER BY f.name""");
		Integer withSecondFaction = jdbcTemplate.queryForObject(
			"SELECT count(*)" + SEED + "AND c.secondary_faction_id IS NOT NULL", Integer.class);

		// Assert
		assertThat(perFaction).hasSize(4).allSatisfy(row -> {
			assertThat(((Number) row.get("creatures")).intValue()).isEqualTo(23);
			assertThat(((Number) row.get("terrains")).intValue()).isEqualTo(1);
			assertThat(((Number) row.get("items")).intValue()).isEqualTo(1);
		});
		assertThat(withSecondFaction).isEqualTo(2);
	}

	@Test
	@DisplayName("Names and image paths are unique, and every non-creature has an effect")
	void catalog_uniqueNamesAndEffects() {
		// Act
		Integer distinctNames = jdbcTemplate.queryForObject("SELECT count(DISTINCT c.name)" + SEED, Integer.class);
		Integer distinctImages = jdbcTemplate.queryForObject("SELECT count(DISTINCT c.image_path)" + SEED, Integer.class);
		Integer withoutDescription = jdbcTemplate.queryForObject(
			"SELECT count(*)" + SEED + "AND c.effect_code IS NOT NULL AND c.effect_description IS NULL", Integer.class);

		// Assert
		assertThat(distinctNames).isEqualTo(120);
		assertThat(distinctImages).isEqualTo(120);
		assertThat(withoutDescription).isZero();
	}
}
