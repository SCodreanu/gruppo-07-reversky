package it.downfall.reversky.api.pack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.collection.service.CollectionService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.inventory.entity.CreditReason;
import it.downfall.reversky.api.inventory.service.InventoryService;
import it.downfall.reversky.api.pack.service.PackService;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Un errore a metà di acquisto o apertura non lascia dati parziali (API-56, #126, docs/test).
 * <p>
 * Gli errori dei test degli endpoint (crediti insufficienti, nessun pacchetto da aprire) arrivano prima di scrivere
 * qualcosa. Qui invece un {@code @MockitoSpyBean} fa fallire l'operazione dopo la prima scrittura della transazione:
 * l'acquisto dopo aver scalato i crediti, l'apertura dopo aver tolto il pacchetto dall'inventario. Dopo la risposta
 * {@code 500} crediti, pacchetti, collezione e {@code pack_openings} devono essere quelli di prima.
 * <p>
 * Gli spy cambiano la configurazione, quindi questa classe ha un contesto Spring suo: per questo è separata da
 * {@link PackFlowIT}.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class PackFlowRollbackIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PackService packService;

	@MockitoSpyBean
	private InventoryService inventoryService;

	@MockitoSpyBean
	private CollectionService collectionService;

	@MockitoBean
	private EmailSender emailSender;

	private User user;
	private Integer packId;

	// Lo spy sta dietro al proxy transazionale: uno stub o una verifica fatti sul proxy passerebbero dall'interceptor
	// delle transazioni e fallirebbero (UnfinishedStubbingException, IllegalTransactionStateException). Stub e
	// verifiche si fanno quindi sullo spy stesso.
	private InventoryService inventorySpy;
	private CollectionService collectionSpy;

	@BeforeEach
	void setUp() {
		inventorySpy = AopTestUtils.getUltimateTargetObject(inventoryService);
		collectionSpy = AopTestUtils.getUltimateTargetObject(collectionService);
		user = testUsers.create();
		packId = jdbcTemplate.queryForObject("SELECT id FROM packs WHERE name = 'Base Pack'", Integer.class);
	}

	@Test
	@DisplayName("A purchase that fails after the credits were spent leaves credits, ledger and packs as before")
	void purchase_failsAfterSpendingCredits_rollsBack() throws Exception {
		// Arrange: il fallimento arriva dopo spendCredits, quando il saldo e la riga PACK_PURCHASE sono già scritti
		doThrow(new IllegalStateException("Simulated failure after the credits were spent"))
			.when(inventorySpy).addPacks(eq(user.getId()), any(), anyInt());
		int creditsBefore = credits();
		int ledgerRowsBefore = ledgerRows();

		// Act
		mockMvc.perform(as(user, post("/api/v1/packs/" + packId + "/purchase")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\": 1}"))
			.andExpect(status().isInternalServerError());

		// Assert
		verify(inventorySpy).spendCredits(eq(user.getId()), anyInt(), eq(CreditReason.PACK_PURCHASE));
		assertThat(credits()).isEqualTo(creditsBefore);
		assertThat(ledgerRows()).isEqualTo(ledgerRowsBefore);
		assertThat(inventoryService.getPackQuantity(user.getId(), packId)).isZero();
	}

	@Test
	@DisplayName("An opening that fails after the pack was taken keeps the pack and adds no card; then it opens fine")
	void open_failsAfterTakingPack_rollsBack() throws Exception {
		// Arrange: un pacchetto comprato davvero, poi il fallimento dopo takePack, prima che le carte siano salvate
		packService.purchase(user.getId(), packId, 1);
		doThrow(new IllegalStateException("Simulated failure after the pack was taken"))
			.when(collectionSpy).addCards(eq(user.getId()), anyMap());
		int creditsBefore = credits();

		// Act
		mockMvc.perform(as(user, post("/api/v1/inventory/packs/" + packId + "/open")))
			.andExpect(status().isInternalServerError());

		// Assert: il pacchetto è ancora lì, nessuna carta e nessuna apertura registrata
		verify(inventorySpy).takePack(user.getId(), packId);
		assertThat(inventoryService.getPackQuantity(user.getId(), packId)).isEqualTo(1);
		assertThat(countRows("collection_cards")).isZero();
		assertThat(countRows("pack_openings")).isZero();
		assertThat(credits()).isEqualTo(creditsBefore);

		// Senza il guasto lo stesso pacchetto si apre normalmente: niente è rimasto a metà
		reset(collectionSpy);
		mockMvc.perform(as(user, post("/api/v1/inventory/packs/" + packId + "/open")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.remainingPacks").value(0));
		assertThat(countRows("pack_openings")).isEqualTo(1);
	}

	private int credits() {
		return jdbcTemplate.queryForObject("SELECT credits FROM inventories WHERE user_id = ?", Integer.class, user.getId());
	}

	private int ledgerRows() {
		return countRows("credit_transactions");
	}

	private int countRows(String table) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE user_id = ?", Integer.class,
			user.getId());
	}

	private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}
}
