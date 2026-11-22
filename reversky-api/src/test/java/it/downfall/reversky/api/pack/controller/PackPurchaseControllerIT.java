package it.downfall.reversky.api.pack.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.inventory.entity.CreditReason;
import it.downfall.reversky.api.inventory.entity.CreditTransaction;
import it.downfall.reversky.api.inventory.repository.CreditTransactionRepository;
import it.downfall.reversky.api.inventory.service.InventoryService;
import it.downfall.reversky.api.pack.dto.PackRequest;
import it.downfall.reversky.api.pack.service.PackAdminService;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Acquisto dei pacchetti via HTTP (API-22): crediti, limite di acquisto, pacchetti nell'inventario.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class PackPurchaseControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private PackAdminService packAdminService;

	@Autowired
	private InventoryService inventoryService;

	@Autowired
	private CreditTransactionRepository creditTransactionRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CatalogCardRepository catalogCardRepository;

	@Autowired
	private RarityRepository rarityRepository;

	@Autowired
	private FactionRepository factionRepository;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	private TestCards testCards;

	@BeforeEach
	void setUp() {
		testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
	}

	@Test
	@DisplayName("Buying packs spends cost x quantity, writes the PACK_PURCHASE row and fills the inventory")
	void purchase_enoughCredits_buysPacks() throws Exception {
		// Arrange
		User user = testUsers.create();
		Integer packId = activePack(100, null);
		int before = inventoryService.getCredits(user.getId());

		// Act + Assert
		purchase(user, packId, 2)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.credits").value(before - 200))
			.andExpect(jsonPath("$.packs[0].packId").value(packId))
			.andExpect(jsonPath("$.packs[0].quantity").value(2))
			.andExpect(jsonPath("$.packs[0].name").isNotEmpty());
		purchase(user, packId, 1).andExpect(jsonPath("$.packs[0].quantity").value(3));
		mockMvc.perform(as(user, get("/api/v1/inventory")))
			.andExpect(jsonPath("$.packs[0].quantity").value(3));
		assertThat(transactions(user)).last()
			.extracting(CreditTransaction::getAmount, CreditTransaction::getReason)
			.containsExactly(-100, CreditReason.PACK_PURCHASE);
	}

	@Test
	@DisplayName("Insufficient credits return 422 and change neither credits nor packs")
	void purchase_insufficientCredits_returns422() throws Exception {
		// Arrange: 2 pacchetti da 400 superano i crediti di benvenuto
		User user = testUsers.create();
		Integer packId = activePack(400, null);
		int before = inventoryService.getCredits(user.getId());

		// Act + Assert
		purchase(user, packId, 2)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("INSUFFICIENT_CREDITS"));
		assertThat(inventoryService.getCredits(user.getId())).isEqualTo(before);
		assertThat(inventoryService.getPackQuantity(user.getId(), packId)).isZero();
	}

	@Test
	@DisplayName("The purchase limit counts opened packs too")
	void purchase_overLimit_countsOpenedPacks() throws Exception {
		// Arrange: limite 3, un pacchetto già aperto (riga in pack_openings)
		User user = testUsers.create();
		Integer packId = activePack(10, 3);
		jdbcTemplate.update("INSERT INTO pack_openings (user_id, pack_id, cards) VALUES (?, ?, '[]'::jsonb)", user.getId(), packId);

		// Act + Assert
		purchase(user, packId, 3)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("PACK_PURCHASE_LIMIT"));
		purchase(user, packId, 2).andExpect(status().isOk());
		purchase(user, packId, 1)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("PACK_PURCHASE_LIMIT"));
	}

	@Test
	@DisplayName("A free pack writes a PACK_PURCHASE row of 0 and respects its limit")
	void purchase_freePack_writesZeroRow() throws Exception {
		// Arrange
		User user = testUsers.create();
		Integer packId = activePack(0, 1);
		int before = inventoryService.getCredits(user.getId());

		// Act + Assert
		purchase(user, packId, 1).andExpect(status().isOk()).andExpect(jsonPath("$.credits").value(before));
		assertThat(transactions(user)).last()
			.extracting(CreditTransaction::getAmount, CreditTransaction::getReason)
			.containsExactly(0, CreditReason.PACK_PURCHASE);
		purchase(user, packId, 1).andExpect(status().isUnprocessableContent());
	}

	@Test
	@DisplayName("GET /packs shows the remaining purchases with a token, null without")
	void shop_remainingPurchases_dependOnUser() throws Exception {
		// Arrange
		User user = testUsers.create();
		Integer packId = activePack(10, 3);
		purchase(user, packId, 1).andExpect(status().isOk());

		// Act + Assert
		mockMvc.perform(as(user, get("/api/v1/packs")))
			.andExpect(jsonPath("$[?(@.id == " + packId + ")].remainingPurchases").value(hasItem(2)));
		mockMvc.perform(get("/api/v1/packs"))
			.andExpect(jsonPath("$[?(@.id == " + packId + ")].remainingPurchases").value(hasItem((Object) null)));
	}

	@Test
	@DisplayName("A deactivated pack returns 422, an unknown one 404, a quantity of 11 returns 400, no token 401")
	void purchase_invalidRequests() throws Exception {
		// Arrange
		User user = testUsers.create();
		Integer packId = activePack(10, null);
		packAdminService.deactivate(packId);

		// Act + Assert
		purchase(user, packId, 1)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("PACK_NOT_AVAILABLE"));
		purchase(user, Integer.MAX_VALUE, 1).andExpect(status().isNotFound());
		purchase(user, packId, 11).andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/v1/packs/" + packId + "/purchase").contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\": 1}"))
			.andExpect(status().isUnauthorized());
	}

	// Pacchetto attivo con uno slot Common (il catalogo V11 ne ha)
	private Integer activePack(int cost, Integer limit) {
		PackRequest request = new PackRequest(TestCards.uniqueName("Pack"), cost, limit, "packs/test.webp",
			List.of(new PackRequest.PackSlotRequest(testCards.common().getId(), 1)), List.of(), null);
		Integer id = packAdminService.create(request).id();
		packAdminService.activate(id);
		return id;
	}

	private ResultActions purchase(User user, Integer packId, int quantity) throws Exception {
		return mockMvc.perform(as(user, post("/api/v1/packs/" + packId + "/purchase"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"quantity\": " + quantity + "}"));
	}

	private List<CreditTransaction> transactions(User user) {
		return creditTransactionRepository.findAllByUserIdOrderByIdAsc(user.getId());
	}

	private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}
}
