package it.downfall.reversky.api.trade.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestTrades;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.trade.entity.TradeStatus;
import it.downfall.reversky.api.trade.repository.TradeRepository;
import it.downfall.reversky.api.user.entity.User;

/**
 * Scambi via HTTP: consultazione, turni e rifiuto (API-31, docs/scambi).
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class TradeControllerIT {

	private static final String BASE_PATH = "/api/v1/trades";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private TradeRepository tradeRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	private TestTrades testTrades;
	private User requester;
	private User recipient;

	@BeforeEach
	void setUp() {
		testTrades = new TestTrades(tradeRepository, jdbcTemplate);
		requester = testUsers.create();
		recipient = testUsers.create();
	}

	// --- liste ---

	@Test
	@DisplayName("A new proposal is incoming for the recipient and outgoing for the requester")
	void findBox_newProposal_isInTheRightBoxes() throws Exception {
		// Arrange
		UUID tradeId = testTrades.create(requester, recipient);

		// Act + Assert
		mockMvc.perform(as(recipient, get(BASE_PATH).param("box", "incoming")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(tradeId.toString()))
			.andExpect(jsonPath("$.content[0].other.id").value(requester.getId()))
			.andExpect(jsonPath("$.content[0].other.username").value(requester.getUsername()))
			.andExpect(jsonPath("$.content[0].status").value("WAITING_RECIPIENT"))
			.andExpect(jsonPath("$.content[0].myTurn").value(true));
		mockMvc.perform(as(requester, get(BASE_PATH).param("box", "outgoing")))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].other.id").value(recipient.getId()))
			.andExpect(jsonPath("$.content[0].myTurn").value(false));
		mockMvc.perform(as(requester, get(BASE_PATH).param("box", "incoming")))
			.andExpect(jsonPath("$.content.length()").value(0));
		mockMvc.perform(as(recipient, get(BASE_PATH).param("box", "outgoing")))
			.andExpect(jsonPath("$.content.length()").value(0));
	}

	@Test
	@DisplayName("After a counter-proposal by the recipient the boxes swap")
	void findBox_waitingRequester_swapsBoxes() throws Exception {
		// Arrange
		UUID tradeId = testTrades.create(requester, recipient);
		testTrades.setStatus(tradeId, TradeStatus.WAITING_REQUESTER);

		// Act + Assert
		mockMvc.perform(as(requester, get(BASE_PATH).param("box", "incoming")))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].myTurn").value(true));
		mockMvc.perform(as(recipient, get(BASE_PATH).param("box", "outgoing")))
			.andExpect(jsonPath("$.content.length()").value(1));
	}

	@Test
	@DisplayName("Closed trades are in no box, latest move comes first")
	void findBox_closedAndOrder() throws Exception {
		// Arrange
		UUID older = testTrades.create(requester, recipient);
		UUID newer = testTrades.create(requester, recipient);
		UUID accepted = testTrades.create(requester, recipient);
		UUID rejected = testTrades.create(requester, recipient);
		testTrades.setUpdatedAt(older, Instant.parse("2026-01-01T10:00:00Z"));
		testTrades.setUpdatedAt(newer, Instant.parse("2026-01-02T10:00:00Z"));
		testTrades.setStatus(accepted, TradeStatus.ACCEPTED);
		testTrades.setStatus(rejected, TradeStatus.REJECTED);

		// Act + Assert
		mockMvc.perform(as(recipient, get(BASE_PATH).param("box", "incoming")))
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].id").value(newer.toString()))
			.andExpect(jsonPath("$.content[1].id").value(older.toString()))
			.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	@DisplayName("An unknown box returns 400 INVALID_TRADE_BOX")
	void findBox_unknownBox_returns400() throws Exception {
		mockMvc.perform(as(requester, get(BASE_PATH).param("box", "closed")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_TRADE_BOX"));
	}

	@Test
	@DisplayName("Without a token the trades return 401")
	void findBox_withoutToken_returns401() throws Exception {
		mockMvc.perform(get(BASE_PATH).param("box", "incoming"))
			.andExpect(status().isUnauthorized());
	}

	// --- dettaglio ---

	@Test
	@DisplayName("Both participants read a trade, also a closed one, each from their own point of view")
	void findById_participants_readTheirView() throws Exception {
		// Arrange
		UUID tradeId = testTrades.create(requester, recipient);
		testTrades.setStatus(tradeId, TradeStatus.ACCEPTED);

		// Act + Assert
		mockMvc.perform(as(requester, get(BASE_PATH + "/" + tradeId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.other.id").value(recipient.getId()))
			.andExpect(jsonPath("$.status").value("ACCEPTED"))
			.andExpect(jsonPath("$.myTurn").value(false));
		mockMvc.perform(as(recipient, get(BASE_PATH + "/" + tradeId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.other.id").value(requester.getId()));
	}

	@Test
	@DisplayName("Someone else's trade and a missing trade both return 404 TRADE_NOT_FOUND")
	void findById_notParticipantOrMissing_returns404() throws Exception {
		// Arrange
		UUID tradeId = testTrades.create(requester, recipient);
		User stranger = testUsers.create();

		// Act + Assert
		mockMvc.perform(as(stranger, get(BASE_PATH + "/" + tradeId)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TRADE_NOT_FOUND"));
		mockMvc.perform(as(requester, get(BASE_PATH + "/" + UUID.randomUUID())))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TRADE_NOT_FOUND"));
	}

	// --- rifiuto ---

	@Test
	@DisplayName("The recipient rejects a proposal: 200 with status REJECTED, and it leaves both boxes")
	void reject_recipient_closesTrade() throws Exception {
		// Arrange
		UUID tradeId = testTrades.create(requester, recipient);

		// Act
		mockMvc.perform(as(recipient, post(BASE_PATH + "/" + tradeId + "/reject")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REJECTED"))
			.andExpect(jsonPath("$.myTurn").value(false));

		// Assert
		assertThat(testTrades.statusOf(tradeId)).isEqualTo(TradeStatus.REJECTED);
		mockMvc.perform(as(recipient, get(BASE_PATH).param("box", "incoming")))
			.andExpect(jsonPath("$.content.length()").value(0));
	}

	@Test
	@DisplayName("The requester withdraws the proposal even when it is not their turn")
	void reject_requesterOutOfTurn_withdraws() throws Exception {
		// Arrange
		UUID tradeId = testTrades.create(requester, recipient);

		// Act + Assert
		mockMvc.perform(as(requester, post(BASE_PATH + "/" + tradeId + "/reject")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	@DisplayName("Rejecting a closed trade returns 409 TRADE_CLOSED and changes nothing")
	void reject_closedTrade_returns409() throws Exception {
		// Arrange
		UUID tradeId = testTrades.create(requester, recipient);
		testTrades.setStatus(tradeId, TradeStatus.ACCEPTED);

		// Act + Assert
		mockMvc.perform(as(recipient, post(BASE_PATH + "/" + tradeId + "/reject")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("TRADE_CLOSED"));
		assertThat(testTrades.statusOf(tradeId)).isEqualTo(TradeStatus.ACCEPTED);
	}

	@Test
	@DisplayName("A stranger cannot reject someone else's trade: 404")
	void reject_stranger_returns404() throws Exception {
		// Arrange
		UUID tradeId = testTrades.create(requester, recipient);

		// Act + Assert
		mockMvc.perform(as(testUsers.create(), post(BASE_PATH + "/" + tradeId + "/reject")))
			.andExpect(status().isNotFound());
		assertThat(testTrades.statusOf(tradeId)).isEqualTo(TradeStatus.WAITING_RECIPIENT);
	}

	private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}
}
