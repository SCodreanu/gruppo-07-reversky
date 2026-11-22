package it.downfall.reversky.api.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.MvcResult;
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
import it.downfall.reversky.api.notification.service.NotificationStreamService;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestCollections;
import it.downfall.reversky.api.support.TestNotifications;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Notifiche degli scambi via HTTP (API-69, docs/notifiche): proposta e controproposta a chi deve rispondere,
 * accettazione e rifiuto all'altra parte, niente per una mossa rifiutata dall'API.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class TradeNotificationIT {

	private static final String PATH = "/api/v1/trades";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CatalogCardRepository catalogCardRepository;

	@Autowired
	private RarityRepository rarityRepository;

	@Autowired
	private FactionRepository factionRepository;

	@Autowired
	private CollectionService collectionService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private NotificationStreamService streamService;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	private TestNotifications testNotifications;
	private User requester;
	private User recipient;
	private CatalogCard offered;
	private CatalogCard asked;

	@BeforeEach
	void setUp() {
		TestCards testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		TestCollections testCollections = new TestCollections(collectionService, transactionTemplate, jdbcTemplate);
		testNotifications = new TestNotifications(jdbcTemplate);
		requester = testUsers.create();
		recipient = testUsers.create();
		offered = testCards.createCreature();
		asked = testCards.createCreature();
		testCollections.give(requester, offered, 2);
		testCollections.give(recipient, asked, 2);
	}

	@AfterEach
	void closeStreams() {
		streamService.disconnect(recipient.getId());
	}

	@Test
	@DisplayName("A proposal notifies the recipient, also on the open stream, with the trade and the requester")
	void propose_notifiesRecipient() throws Exception {
		// Arrange: il destinatario ha il canale aperto
		MvcResult stream = mockMvc.perform(as(recipient, get("/api/v1/notifications/stream"))
				.accept(MediaType.TEXT_EVENT_STREAM))
			.andExpect(request().asyncStarted())
			.andReturn();

		// Act
		String tradeId = propose();

		// Assert
		assertThat(testNotifications.types(recipient)).containsExactly("TRADE_PROPOSED");
		assertThat(testNotifications.lastPayload(recipient, "TRADE_PROPOSED"))
			.contains("\"tradeId\": \"" + tradeId + "\"")
			.contains("\"userId\": " + requester.getId())
			.contains("\"username\": \"" + requester.getUsername() + "\"");
		assertThat(testNotifications.types(requester)).isEmpty();
		assertThat(stream.getResponse().getContentAsString())
			.contains("event:TRADE_PROPOSED\n")
			.contains("\"tradeId\":\"" + tradeId + "\"");
	}

	@Test
	@DisplayName("A counter notifies the one who must answer; accepting notifies the other party")
	void counter_thenAccept_notifiesEachOther() throws Exception {
		// Arrange
		String tradeId = propose();

		// Act: il destinatario contropropone, il richiedente accetta
		mockMvc.perform(as(recipient, post(PATH + "/" + tradeId + "/counter")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"give\": [" + card(asked, 1) + "], \"receive\": [" + card(offered, 2) + "]}"))
			.andExpect(status().isOk());
		mockMvc.perform(as(requester, post(PATH + "/" + tradeId + "/accept"))).andExpect(status().isOk());

		// Assert
		assertThat(testNotifications.types(requester)).containsExactly("TRADE_COUNTERED");
		assertThat(testNotifications.lastPayload(requester, "TRADE_COUNTERED"))
			.contains("\"username\": \"" + recipient.getUsername() + "\"");
		assertThat(testNotifications.types(recipient)).containsExactly("TRADE_PROPOSED", "TRADE_ACCEPTED");
		assertThat(testNotifications.lastPayload(recipient, "TRADE_ACCEPTED"))
			.contains("\"tradeId\": \"" + tradeId + "\"")
			.contains("\"username\": \"" + requester.getUsername() + "\"");
	}

	@Test
	@DisplayName("Rejecting notifies the other party, also when the requester withdraws out of turn")
	void reject_notifiesOtherParty() throws Exception {
		// Act: il destinatario rifiuta la prima, il richiedente ritira la seconda
		String rejected = propose();
		mockMvc.perform(as(recipient, post(PATH + "/" + rejected + "/reject"))).andExpect(status().isOk());
		String withdrawn = propose();
		mockMvc.perform(as(requester, post(PATH + "/" + withdrawn + "/reject"))).andExpect(status().isOk());

		// Assert
		assertThat(testNotifications.types(requester)).containsExactly("TRADE_REJECTED");
		assertThat(testNotifications.lastPayload(requester, "TRADE_REJECTED"))
			.contains("\"tradeId\": \"" + rejected + "\"");
		assertThat(testNotifications.types(recipient)).containsExactly("TRADE_PROPOSED", "TRADE_PROPOSED",
			"TRADE_REJECTED");
		assertThat(testNotifications.lastPayload(recipient, "TRADE_REJECTED"))
			.contains("\"tradeId\": \"" + withdrawn + "\"")
			.contains("\"username\": \"" + requester.getUsername() + "\"");
	}

	@Test
	@DisplayName("A move refused by the API (accept out of turn, accept of a closed trade) notifies nobody")
	void refusedMove_notifiesNobody() throws Exception {
		// Arrange
		String tradeId = propose();

		// Act: il richiedente prova ad accettare la sua proposta, poi il destinatario rifiuta e prova ad accettare
		mockMvc.perform(as(requester, post(PATH + "/" + tradeId + "/accept"))).andExpect(status().isConflict());
		mockMvc.perform(as(recipient, post(PATH + "/" + tradeId + "/reject"))).andExpect(status().isOk());
		mockMvc.perform(as(recipient, post(PATH + "/" + tradeId + "/accept"))).andExpect(status().isConflict());

		// Assert: solo la proposta e il rifiuto
		assertThat(testNotifications.types(recipient)).containsExactly("TRADE_PROPOSED");
		assertThat(testNotifications.types(requester)).containsExactly("TRADE_REJECTED");
	}

	private String propose() throws Exception {
		String json = "{\"recipientId\": " + recipient.getId() + ", \"give\": [" + card(offered, 1) + "], \"receive\": ["
			+ card(asked, 1) + "]}";
		return JsonPath.read(mockMvc.perform(as(requester, post(PATH)).contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString(), "$.id");
	}

	private static String card(CatalogCard card, int quantity) {
		return "{\"cardId\": " + card.getId() + ", \"quantity\": " + quantity + "}";
	}

	private MockHttpServletRequestBuilder as(User who, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(who));
	}
}
