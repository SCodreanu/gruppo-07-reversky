package it.downfall.reversky.api.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.collection.service.CollectionService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestCollections;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Flusso completo degli scambi via HTTP con il database vero (API-57, #127): proposta, controproposte e accettazione,
 * come li farà il frontend. I singoli passi e i casi di errore sono già in {@code TradeProposalControllerIT},
 * {@code TradeControllerIT} e {@code TradeAcceptIT}; qui si controlla che, lungo tutta la trattativa, le riserve
 * seguano l'offerta in corso e che il totale delle carte nel sistema non cambi mai (docs/test, "Cosa deve essere
 * coperto").
 * <p>
 * Le carte sono create dal test, quindi le possiedono solo gli utenti del test: la somma di {@code collection_cards}
 * per carta è il totale nel sistema.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class TradeFlowIT {

	private static final String BASE_PATH = "/api/v1/trades";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private CollectionService collectionService;

	@Autowired
	private CatalogCardRepository catalogCardRepository;

	@Autowired
	private RarityRepository rarityRepository;

	@Autowired
	private FactionRepository factionRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT degli scambi Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	private TestCollections testCollections;
	private User requester;
	private User recipient;
	private CatalogCard crawlers;
	private CatalogCard mothman;
	private CatalogCard wendigo;

	@BeforeEach
	void setUp() {
		TestCards testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		testCollections = new TestCollections(collectionService, transactionTemplate, jdbcTemplate);
		requester = testUsers.create();
		recipient = testUsers.create();
		crawlers = testCards.createCreature();
		mothman = testCards.createCreature();
		wendigo = testCards.createCreature();
		testCollections.give(requester, crawlers, 3);
		testCollections.give(recipient, mothman, 2);
		testCollections.give(recipient, wendigo, 1);
	}

	@Test
	@DisplayName("Propose, counter twice and accept: reservations follow the current offer, the last offer moves, the total holds")
	void negotiation_proposeCounterCounterAccept() throws Exception {
		// Arrange
		Map<CatalogCard, Integer> totalsBefore = totals();

		// Act + Assert, passo per passo
		// 1. requester propone 1 Crawlers contro 1 Mothman: la sua copia offerta è riservata
		String tradeId = JsonPath.read(propose(requester, recipient,
				"[" + card(crawlers, 1) + "]", "[" + card(mothman, 1) + "]")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("WAITING_RECIPIENT"))
			.andReturn().getResponse().getContentAsString(), "$.id");
		assertReserved(requester, crawlers, 1);
		assertReserved(recipient, mothman, 0);

		// 2. recipient contropropone: 1 Mothman contro 2 Crawlers; la riserva passa a lui
		counter(recipient, tradeId, "[" + card(mothman, 1) + "]", "[" + card(crawlers, 2) + "]")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("WAITING_REQUESTER"));
		assertReserved(requester, crawlers, 0);
		assertReserved(recipient, mothman, 1);

		// 3. requester contropropone: 2 Crawlers contro 1 Mothman e 1 Wendigo
		counter(requester, tradeId, "[" + card(crawlers, 2) + "]", "[" + card(mothman, 1) + ", " + card(wendigo, 1) + "]")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("WAITING_RECIPIENT"));
		assertReserved(requester, crawlers, 2);
		assertReserved(recipient, mothman, 0);

		// 4. recipient accetta l'ultima offerta
		mockMvc.perform(as(recipient, post(BASE_PATH + "/" + tradeId + "/accept")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACCEPTED"));

		// Si sposta l'ultima offerta, non le precedenti
		assertThat(testCollections.quantity(requester, crawlers)).isEqualTo(1);
		assertThat(testCollections.quantity(requester, mothman)).isEqualTo(1);
		assertThat(testCollections.quantity(requester, wendigo)).isEqualTo(1);
		assertThat(testCollections.quantity(recipient, crawlers)).isEqualTo(2);
		assertThat(testCollections.quantity(recipient, mothman)).isEqualTo(1);
		assertThat(testCollections.quantity(recipient, wendigo)).isZero();
		assertThat(totals()).isEqualTo(totalsBefore);
		assertReserved(requester, crawlers, 0);
		assertReserved(recipient, crawlers, 0);

		// Lo scambio chiuso non è in nessuna casella, ma entrambi lo leggono
		for (User user : List.of(requester, recipient)) {
			for (String box : List.of("incoming", "outgoing")) {
				mockMvc.perform(as(user, get(BASE_PATH)).param("box", box))
					.andExpect(jsonPath("$.totalElements").value(0));
			}
			mockMvc.perform(as(user, get(BASE_PATH + "/" + tradeId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACCEPTED"));
		}
	}

	@Test
	@DisplayName("Copies received in one trade can be offered in the next; the total holds across both trades")
	void chain_receivedCopiesCanBeTradedOn() throws Exception {
		// Arrange
		User third = testUsers.create();
		Map<CatalogCard, Integer> totalsBefore = totals();

		// Act
		// 1. requester cede 2 Crawlers a recipient per 1 Mothman, e recipient accetta
		String first = JsonPath.read(propose(requester, recipient,
				"[" + card(crawlers, 2) + "]", "[" + card(mothman, 1) + "]")
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
		mockMvc.perform(as(recipient, post(BASE_PATH + "/" + first + "/accept"))).andExpect(status().isOk());

		// 2. recipient regala a third i 2 Crawlers appena ricevuti, e third accetta
		String second = JsonPath.read(propose(recipient, third, "[" + card(crawlers, 2) + "]", "[]")
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
		mockMvc.perform(as(third, post(BASE_PATH + "/" + second + "/accept")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACCEPTED"));

		// Assert
		assertThat(testCollections.quantity(requester, crawlers)).isEqualTo(1);
		assertThat(testCollections.quantity(recipient, crawlers)).isZero();
		assertThat(testCollections.quantity(third, crawlers)).isEqualTo(2);
		assertThat(testCollections.quantity(requester, mothman)).isEqualTo(1);
		assertThat(testCollections.quantity(recipient, mothman)).isEqualTo(1);
		assertThat(totals()).isEqualTo(totalsBefore);
	}

	// Copie riservate di una carta, lette come le legge il frontend: GET /collection filtrato per nome
	private void assertReserved(User user, CatalogCard card, int expected) throws Exception {
		mockMvc.perform(as(user, get("/api/v1/collection")).param("name", card.getName()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[?(@.card.id == " + card.getId() + ")].reservedQuantity").value(expected));
	}

	// Copie di ogni carta del test in tutto il sistema
	private Map<CatalogCard, Integer> totals() {
		return Map.of(crawlers, total(crawlers), mothman, total(mothman), wendigo, total(wendigo));
	}

	private int total(CatalogCard card) {
		return jdbcTemplate.queryForObject("SELECT COALESCE(SUM(quantity), 0) FROM collection_cards WHERE card_id = ?",
			Integer.class, card.getId());
	}

	private ResultActions propose(User from, User to, String give, String receive) throws Exception {
		String json = "{\"recipientId\": " + to.getId() + ", \"give\": " + give + ", \"receive\": " + receive + "}";
		return mockMvc.perform(as(from, post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions counter(User from, String tradeId, String give, String receive) throws Exception {
		String json = "{\"give\": " + give + ", \"receive\": " + receive + "}";
		return mockMvc.perform(as(from, post(BASE_PATH + "/" + tradeId + "/counter"))
			.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static String card(CatalogCard card, int quantity) {
		return "{\"cardId\": " + card.getId() + ", \"quantity\": " + quantity + "}";
	}

	private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}
}
