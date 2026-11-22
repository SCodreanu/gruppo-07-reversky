package it.downfall.reversky.api.pack.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.inventory.service.InventoryService;
import it.downfall.reversky.api.pack.dto.PackRequest;
import it.downfall.reversky.api.pack.dto.PackRequest.PackBonusRequest;
import it.downfall.reversky.api.pack.dto.PackRequest.PackSlotRequest;
import it.downfall.reversky.api.pack.entity.PackOpening;
import it.downfall.reversky.api.pack.repository.PackOpeningRepository;
import it.downfall.reversky.api.pack.service.PackAdminService;
import it.downfall.reversky.api.pack.service.PackService;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Apertura dei pacchetti via HTTP (API-24): carte nella collezione, riga in pack_openings, isNew, pacchetto tolto
 * dall'inventario. Le regole di slot, bonus e ripiego sono in PackDrawerTest e CardPoolTest.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class PackOpeningControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private PackAdminService packAdminService;

	@Autowired
	private PackService packService;

	@Autowired
	private InventoryService inventoryService;

	@Autowired
	private PackOpeningRepository packOpeningRepository;

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
	@DisplayName("Opening gives as many cards as the base slots, adds them to the collection and records the opening")
	void open_ownedPack_addsCardsAndRecordsOpening() throws Exception {
		// Arrange
		User user = testUsers.create();
		Integer packId = basePack();
		packService.purchase(user.getId(), packId, 2);

		// Act
		String body = open(user, packId)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.packId").value(packId))
			.andExpect(jsonPath("$.remainingPacks").value(1))
			.andExpect(jsonPath("$.cards.length()").value(7))
			.andReturn().getResponse().getContentAsString();

		// Assert: stesse carte nella risposta, nella collezione e in pack_openings, in ordine di slot
		List<Integer> cardIds = JsonPath.read(body, "$.cards[*].card.id");
		Map<Integer, Integer> expected = new LinkedHashMap<>();
		cardIds.forEach(id -> expected.merge(id, 1, Integer::sum));
		assertThat(collection(user)).containsExactlyInAnyOrderEntriesOf(expected);
		List<PackOpening> openings = packOpeningRepository.findAllByUserIdOrderByIdAsc(user.getId());
		assertThat(openings).hasSize(1);
		assertThat(openings.get(0).getCards()).containsExactlyElementsOf(cardIds);
		List<Integer> orders = JsonPath.read(body, "$.cards[*].card.rarity.id");
		assertThat(orders).isNotEmpty();
		assertThat(inventoryService.getPackQuantity(user.getId(), packId)).isEqualTo(1);
	}

	@Test
	@DisplayName("isNew is true only for cards the user did not own, and only the first time in the pack")
	void open_isNew_onlyForFirstNewCopy() throws Exception {
		// Arrange
		User user = testUsers.create();
		Integer packId = basePack();
		packService.purchase(user.getId(), packId, 2);
		open(user, packId).andExpect(status().isOk());
		Set<Integer> ownedBefore = collection(user).keySet();

		// Act
		String body = open(user, packId).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		// Assert
		List<Integer> cardIds = JsonPath.read(body, "$.cards[*].card.id");
		List<Boolean> isNew = JsonPath.read(body, "$.cards[*].isNew");
		Set<Integer> seen = new HashSet<>(ownedBefore);
		for (int i = 0; i < cardIds.size(); i++) {
			assertThat(isNew.get(i)).as("card %d", i).isEqualTo(seen.add(cardIds.get(i)));
		}
	}

	@Test
	@DisplayName("Opening the last pack deletes the inventory row")
	void open_lastPack_deletesRow() throws Exception {
		// Arrange
		User user = testUsers.create();
		Integer packId = basePack();
		packService.purchase(user.getId(), packId, 1);

		// Act + Assert
		open(user, packId).andExpect(jsonPath("$.remainingPacks").value(0));
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM pack_inventory WHERE user_id = ?", Integer.class,
			user.getId())).isZero();
	}

	@Test
	@DisplayName("Without packs to open the answer is 422 NO_PACK_TO_OPEN and nothing changes")
	void open_noPack_returns422() throws Exception {
		// Arrange
		User user = testUsers.create();
		Integer packId = basePack();

		// Act + Assert
		open(user, packId)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("NO_PACK_TO_OPEN"));
		assertThat(collection(user)).isEmpty();
		assertThat(packOpeningRepository.findAllByUserIdOrderByIdAsc(user.getId())).isEmpty();
	}

	@Test
	@DisplayName("A pack bought before its deactivation can still be opened")
	void open_deactivatedPack_stillOpens() throws Exception {
		// Arrange
		User user = testUsers.create();
		Integer packId = basePack();
		packService.purchase(user.getId(), packId, 1);
		packAdminService.deactivate(packId);

		// Act + Assert
		open(user, packId).andExpect(status().isOk()).andExpect(jsonPath("$.cards.length()").value(7));
	}

	@Test
	@DisplayName("Without a token opening returns 401")
	void open_withoutToken_returns401() throws Exception {
		mockMvc.perform(post("/api/v1/inventory/packs/1/open")).andExpect(status().isUnauthorized());
	}

	// Come il pacchetto base: 3 Common, 2 Uncommon, 2 Rare, bonus Mythic 10%
	private Integer basePack() {
		Integer mythicId = rarityRepository.findAll().stream().filter(r -> r.getSortingOrder() == 4).findFirst()
			.orElseThrow().getId();
		PackRequest request = new PackRequest(TestCards.uniqueName("Pack"), 10, null, "packs/test.webp",
			List.of(new PackSlotRequest(testCards.common().getId(), 3), new PackSlotRequest(testCards.uncommon().getId(), 2),
				new PackSlotRequest(testCards.rare().getId(), 2)),
			List.of(new PackBonusRequest(mythicId, 1000)), null);
		Integer id = packAdminService.create(request).id();
		packAdminService.activate(id);
		return id;
	}

	private ResultActions open(User user, Integer packId) throws Exception {
		return mockMvc.perform(as(user, post("/api/v1/inventory/packs/" + packId + "/open")));
	}

	private Map<Integer, Integer> collection(User user) {
		Map<Integer, Integer> rows = new LinkedHashMap<>();
		jdbcTemplate.query("SELECT card_id, quantity FROM collection_cards WHERE user_id = ?",
			rs -> {
				rows.put(rs.getInt("card_id"), rs.getInt("quantity"));
			}, user.getId());
		return rows;
	}

	private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}
}
