package it.downfall.reversky.api.pack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.support.TestUsers;

/**
 * Flusso completo dei pacchetti via HTTP con il database vero (API-56, #126): registrazione, login, crediti di
 * benvenuto, acquisto e apertura della Mini di {@code V16}, collezione. I singoli endpoint e i loro errori sono già
 * in {@code PackPurchaseControllerIT}, {@code PackOpeningControllerIT} e {@code InventoryServiceIT}; qui si controlla
 * che crediti e carte tornino a ogni passo, come li vedrà il frontend. Gli errori a metà operazione sono in
 * {@link PackFlowRollbackIT}.
 * <p>
 * I crediti si controllano due volte: il saldo restituito dall'API e la somma delle righe di
 * {@code credit_transactions}, che devono coincidere sempre.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class PackFlowIT {

	// La bustina più economica di V16: i crediti di benvenuto la pagano
	private static final String PACK = "Mini";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RuleSetService ruleSetService;

	// La registrazione manda l'email di verifica: con la stessa configurazione degli altri IT Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("Register, log in, buy a Mini, open it: credits and cards add up at every step")
	void packFlow_fromRegistrationToCollection() throws Exception {
		// 1. Registrazione e login, come un giocatore nuovo
		String suffix = TestUsers.uniqueSuffix();
		String email = "flow_" + suffix + "@example.com";
		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
				{"username": "flow_%s", "email": "%s", "password": "%s", "acceptPrivacy": true, "acceptTerms": true}
				""".formatted(suffix, email, TestUsers.PASSWORD)))
			.andExpect(status().isCreated());
		String token = JsonPath.read(mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, TestUsers.PASSWORD)))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString(), "$.accessToken");
		Integer userId = JsonPath.read(mockMvc.perform(as(token, get("/api/v1/users/me")))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString(), "$.id");

		// 2. Crediti di benvenuto del rule set attivo, con la riga SIGNUP, e nessun pacchetto
		int signupBonus = ruleSetService.getActiveRuleSet().getSignupBonus();
		mockMvc.perform(as(token, get("/api/v1/users/me"))).andExpect(jsonPath("$.credits").value(signupBonus));
		mockMvc.perform(as(token, get("/api/v1/inventory")))
			.andExpect(jsonPath("$.credits").value(signupBonus))
			.andExpect(jsonPath("$.packs.length()").value(0));
		assertThat(ledger(userId, "SIGNUP")).isEqualTo(signupBonus);
		assertLedgerMatchesBalance(userId, signupBonus);

		// 3. La Mini nel negozio, con costo e numero di carte
		String shop = mockMvc.perform(as(token, get("/api/v1/packs")))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		List<Integer> ids = JsonPath.read(shop, "$[?(@.name == '" + PACK + "')].id");
		assertThat(ids).hasSize(1);
		Integer packId = ids.get(0);
		int cost = JsonPath.<List<Integer>>read(shop, "$[?(@.name == '" + PACK + "')].cost").get(0);
		int cardCount = JsonPath.<List<Integer>>read(shop, "$[?(@.name == '" + PACK + "')].cardCount").get(0);
		assertThat(signupBonus).as("the welcome credits pay for a Mini").isGreaterThanOrEqualTo(cost);

		// 4. Acquisto di un pacchetto: crediti scalati, riga PACK_PURCHASE, pacchetto nell'inventario
		int afterPurchase = signupBonus - cost;
		mockMvc.perform(as(token, post("/api/v1/packs/" + packId + "/purchase")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\": 1}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.credits").value(afterPurchase))
			.andExpect(jsonPath("$.packs[?(@.packId == " + packId + ")].quantity").value(1));
		assertThat(ledger(userId, "PACK_PURCHASE")).isEqualTo(-cost);
		assertLedgerMatchesBalance(userId, afterPurchase);

		// 5. Apertura del pacchetto: ogni carta della risposta finisce nella collezione
		Map<Integer, Integer> drawn = new HashMap<>();
		for (int remaining : List.of(0)) {
			String opened = mockMvc.perform(as(token, post("/api/v1/inventory/packs/" + packId + "/open")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.packId").value(packId))
				.andExpect(jsonPath("$.remainingPacks").value(remaining))
				.andExpect(jsonPath("$.cards.length()").value(cardCount))
				.andReturn().getResponse().getContentAsString();
			JsonPath.<List<Integer>>read(opened, "$.cards[*].card.id").forEach(id -> drawn.merge(id, 1, Integer::sum));
		}
		assertThat(collection(token)).isEqualTo(drawn);
		assertThat(drawn.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(cardCount);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pack_openings WHERE user_id = ? AND pack_id = ?",
			Integer.class, userId, packId)).isEqualTo(1);

		// 6. Aprire non costa crediti; l'inventario è vuoto e un'altra apertura è rifiutata senza cambiare niente
		mockMvc.perform(as(token, get("/api/v1/inventory")))
			.andExpect(jsonPath("$.credits").value(afterPurchase))
			.andExpect(jsonPath("$.packs.length()").value(0));
		assertLedgerMatchesBalance(userId, afterPurchase);
		mockMvc.perform(as(token, post("/api/v1/inventory/packs/" + packId + "/open")))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("NO_PACK_TO_OPEN"));
		assertThat(collection(token)).isEqualTo(drawn);
	}

	// Le carte possedute, lette come le legge il frontend: card.id -> quantità
	private Map<Integer, Integer> collection(String token) throws Exception {
		String body = mockMvc.perform(as(token, get("/api/v1/collection")).param("size", "100"))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		List<Integer> ids = JsonPath.read(body, "$.content[*].card.id");
		List<Integer> quantities = JsonPath.read(body, "$.content[*].quantity");
		Map<Integer, Integer> owned = new HashMap<>();
		for (int i = 0; i < ids.size(); i++) {
			owned.put(ids.get(i), quantities.get(i));
		}
		return owned;
	}

	// Somma delle righe di credit_transactions di un motivo
	private int ledger(Integer userId, String reason) {
		return jdbcTemplate.queryForObject("""
			SELECT COALESCE(SUM(amount), 0) FROM credit_transactions WHERE user_id = ? AND reason::text = ?
			""", Integer.class, userId, reason);
	}

	// Il saldo dell'inventario è sempre la somma di tutte le righe di credit_transactions
	private void assertLedgerMatchesBalance(Integer userId, int balance) {
		Integer ledger = jdbcTemplate.queryForObject(
			"SELECT COALESCE(SUM(amount), 0) FROM credit_transactions WHERE user_id = ?", Integer.class, userId);
		Integer credits = jdbcTemplate.queryForObject("SELECT credits FROM inventories WHERE user_id = ?", Integer.class,
			userId);
		assertThat(credits).isEqualTo(balance);
		assertThat(ledger).isEqualTo(balance);
	}

	private static MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
	}
}
