package it.downfall.reversky.api.migration;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Pacchetto base della migrazione {@code V12} (API-63, #133): in vendita su un database vuoto, si compra e si apre.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class BasePackMigrationIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// Non serve al test: stessa configurazione degli altri ControllerIT, così Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("The base pack is on sale: 100 credits, 3 Common, 2 Uncommon, 2 Rare and three bonuses")
	void basePack_isOnSale() throws Exception {
		// Arrange
		Integer id = basePackId();

		// Act + Assert
		mockMvc.perform(get("/api/v1/packs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.id == " + id + ")].cost").value(hasItem(100)))
			.andExpect(jsonPath("$[?(@.id == " + id + ")].cardCount").value(hasItem(7)))
			.andExpect(jsonPath("$[?(@.id == " + id + ")].slots[*].quantity").value(contains(3, 2, 2)))
			.andExpect(jsonPath("$[?(@.id == " + id + ")].bonuses[*].probabilityUnits").value(contains(1000, 500, 250)));
	}

	@Test
	@DisplayName("A new user buys and opens the base pack")
	void basePack_buyAndOpen() throws Exception {
		// Arrange
		User user = testUsers.create();
		Integer id = basePackId();
		String token = "Bearer " + jwtService.createAccessToken(user);

		// Act + Assert
		mockMvc.perform(post("/api/v1/packs/" + id + "/purchase").header(HttpHeaders.AUTHORIZATION, token)
				.contentType(MediaType.APPLICATION_JSON).content("{\"quantity\": 1}"))
			.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/inventory/packs/" + id + "/open").header(HttpHeaders.AUTHORIZATION, token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cards.length()").value(7));
	}

	private Integer basePackId() {
		return jdbcTemplate.queryForObject("SELECT id FROM packs WHERE name = 'Base Pack'", Integer.class);
	}
}
