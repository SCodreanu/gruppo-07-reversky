package it.downfall.reversky.api.migration;

import static org.assertj.core.api.Assertions.assertThat;
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
 * Bustine del negozio dopo {@code V16} (proposta DSG "Economia di gioco 2.0"): Mini, Standard (il Base Pack di
 * {@code V12}) e Premium in vendita su un database vuoto; un utente nuovo compra e apre una Mini.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class ShopPacksMigrationIT {

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
	@DisplayName("Mini is on sale: 300 credits, 1 Common, 1 Uncommon, 1 Rare, bonuses 7%, 2.5%, 0.5%")
	void mini_isOnSale() throws Exception {
		assertPack("Mini", 300, 3, new Integer[] {1, 1, 1}, new Integer[] {700, 250, 50});
	}

	@Test
	@DisplayName("Standard, the former Base Pack, is on sale: 600 credits, 3 Common, 2 Uncommon, 2 Rare, bonuses 10%, 5%, 2.5%")
	void standard_isOnSale() throws Exception {
		assertPack("Standard", 600, 7, new Integer[] {3, 2, 2}, new Integer[] {1000, 500, 250});
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM packs WHERE name = 'Base Pack'", Integer.class))
			.isZero();
	}

	@Test
	@DisplayName("Premium is on sale: 1200 credits, 5 Common, 4 Uncommon, 6 Rare, bonuses over three flexible slots")
	void premium_isOnSale() throws Exception {
		assertPack("Premium", 1200, 15, new Integer[] {5, 4, 6}, new Integer[] {5254, 2710, 873});
	}

	@Test
	@DisplayName("A new user buys and opens a Mini with the welcome credits")
	void mini_buyAndOpen() throws Exception {
		// Arrange
		User user = testUsers.create();
		Integer id = packId("Mini");
		String token = "Bearer " + jwtService.createAccessToken(user);

		// Act + Assert
		mockMvc.perform(post("/api/v1/packs/" + id + "/purchase").header(HttpHeaders.AUTHORIZATION, token)
				.contentType(MediaType.APPLICATION_JSON).content("{\"quantity\": 1}"))
			.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/inventory/packs/" + id + "/open").header(HttpHeaders.AUTHORIZATION, token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cards.length()").value(3));
	}

	private void assertPack(String name, int cost, int cardCount, Integer[] slots, Integer[] bonusUnits) throws Exception {
		Integer id = packId(name);
		mockMvc.perform(get("/api/v1/packs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.id == " + id + ")].cost").value(hasItem(cost)))
			.andExpect(jsonPath("$[?(@.id == " + id + ")].cardCount").value(hasItem(cardCount)))
			.andExpect(jsonPath("$[?(@.id == " + id + ")].slots[*].quantity").value(contains(slots)))
			.andExpect(jsonPath("$[?(@.id == " + id + ")].bonuses[*].probabilityUnits").value(contains(bonusUnits)));
	}

	private Integer packId(String name) {
		return jdbcTemplate.queryForObject("SELECT id FROM packs WHERE name = ?", Integer.class, name);
	}
}
