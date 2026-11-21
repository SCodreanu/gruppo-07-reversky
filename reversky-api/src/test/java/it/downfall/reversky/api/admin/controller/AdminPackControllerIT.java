package it.downfall.reversky.api.admin.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link AdminPackController} e la lista pubblica GET /packs via HTTP (API-18, API-19, API-20). Il catalogo di
 * {@code V11} ha carte di ogni rarità del seed, quindi i pacchetti con slot Common, Uncommon e Rare si attivano.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class AdminPackControllerIT {

	private static final String BASE_PATH = "/api/v1/admin/packs";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

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
	private Rarity common;
	private Rarity uncommon;
	private Rarity rare;
	private Rarity mythic;

	@BeforeEach
	void setUp() {
		testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		common = testCards.common();
		uncommon = testCards.uncommon();
		rare = testCards.rare();
		mythic = rarityRepository.findAll().stream().filter(r -> r.getSortingOrder() == 4).findFirst().orElseThrow();
	}

	@Test
	@DisplayName("A USER cannot manage packs")
	void create_userRole_returns403() throws Exception {
		mockMvc.perform(asUser(post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content(validBody(uniqueName())))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("A new pack is saved deactivated, with ordered slots, card count and bonus percentages")
	void create_validPack_returns201Deactivated() throws Exception {
		// Arrange
		String name = uniqueName();

		// Act + Assert
		create(validBody(name))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value(name))
			.andExpect(jsonPath("$.active").value(false))
			.andExpect(jsonPath("$.cardCount").value(7))
			.andExpect(jsonPath("$.slots[*].rarity.id").value(contains(common.getId(), uncommon.getId(), rare.getId())))
			.andExpect(jsonPath("$.slots[*].quantity").value(contains(3, 2, 2)))
			.andExpect(jsonPath("$.bonuses[0].rarity.id").value(mythic.getId()))
			.andExpect(jsonPath("$.bonuses[0].probabilityUnits").value(1000))
			.andExpect(jsonPath("$.bonuses[0].percentage").value(10.0));
	}

	@Test
	@DisplayName("A duplicate name returns 409")
	void create_duplicateName_returns409() throws Exception {
		String name = uniqueName();
		create(validBody(name)).andExpect(status().isCreated());

		create(validBody(name))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PACK_NAME_ALREADY_USED"));
	}

	@Test
	@DisplayName("Invalid fields return 400: free pack without limit, no slots, repeated rarity, odds over 10000")
	void create_invalidFields_returns400() throws Exception {
		create(body(uniqueName(), 0, null, slots(common, 1), "[]"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(hasItem("freePackLimited")));
		create(body(uniqueName(), 100, null, "[]", "[]"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(hasItem("slots")));
		create(body(uniqueName(), 100, null, "[" + slot(common, 1) + ", " + slot(common, 2) + "]", "[]"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(hasItem("slotRaritiesUnique")));
		create(body(uniqueName(), 100, null, slots(common, 1), "[" + bonus(mythic, 10_001) + "]"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(hasItem("bonuses[0].probabilityUnits")));
	}

	@Test
	@DisplayName("A free pack with a limit is accepted")
	void create_freePackWithLimit_returns201() throws Exception {
		create(body(uniqueName(), 0, 1, slots(common, 1), "[]"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.cost").value(0))
			.andExpect(jsonPath("$.purchaseLimit").value(1));
	}

	@Test
	@DisplayName("An unknown rarity returns 422 PACK_RARITY_INVALID; a bonus not rarer than every slot returns 422")
	void create_invalidRarities_returns422() throws Exception {
		create(body(uniqueName(), 100, null, "[{\"rarityId\": " + Integer.MAX_VALUE + ", \"quantity\": 1}]", "[]"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("PACK_RARITY_INVALID"));
		create(body(uniqueName(), 100, null, slots(rare, 1), "[" + bonus(uncommon, 500) + "]"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("PACK_RULES_VIOLATED"));
		create(body(uniqueName(), 100, null, slots(rare, 1), "[" + bonus(rare, 500) + "]"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("PACK_RULES_VIOLATED"));
	}

	@Test
	@DisplayName("Activation puts the pack in GET /packs (public), deactivation removes it")
	void activateAndDeactivate_changeShop() throws Exception {
		// Arrange
		Integer id = createdId(validBody(uniqueName()));

		// Act + Assert
		mockMvc.perform(get("/api/v1/packs")).andExpect(jsonPath("$[*].id").value(not(hasItem(id))));
		mockMvc.perform(asAdmin(post(BASE_PATH + "/" + id + "/activate")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.active").value(true));
		mockMvc.perform(get("/api/v1/packs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.id == " + id + ")].cardCount").value(hasItem(7)))
			.andExpect(jsonPath("$[?(@.id == " + id + ")].remainingPurchases").value(hasItem((Object) null)));
		mockMvc.perform(asAdmin(post(BASE_PATH + "/" + id + "/deactivate")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.active").value(false));
		mockMvc.perform(get("/api/v1/packs")).andExpect(jsonPath("$[*].id").value(not(hasItem(id))));
	}

	@Test
	@DisplayName("GET /packs hides a bonus whose rarity has no cards; the backoffice still shows it")
	void shop_bonusWithoutCards_isHidden() throws Exception {
		// Arrange: una rarità nuova, senza carte, come bonus
		Rarity empty = testCards.createRarity();
		Integer id = createdId(body(uniqueName(), 100, null, slots(common, 1), "[" + bonus(empty, 500) + "]"));
		mockMvc.perform(asAdmin(post(BASE_PATH + "/" + id + "/activate"))).andExpect(status().isOk());

		// Act + Assert
		mockMvc.perform(get("/api/v1/packs"))
			.andExpect(jsonPath("$[?(@.id == " + id + ")].bonuses[*]").value(empty()));
		mockMvc.perform(asAdmin(get(BASE_PATH)))
			.andExpect(jsonPath("$[?(@.id == " + id + ")].bonuses[*].rarity.id").value(hasItem(empty.getId())));
	}

	@Test
	@DisplayName("Updating replaces fields, slots and bonuses, also on an active pack")
	void update_activePack_replacesEverything() throws Exception {
		// Arrange
		Integer id = createdId(validBody(uniqueName()));
		mockMvc.perform(asAdmin(post(BASE_PATH + "/" + id + "/activate"))).andExpect(status().isOk());
		String newName = uniqueName();
		String json = body(newName, 150, 5, "[" + slot(common, 4) + ", " + slot(rare, 1) + "]", "[]");

		// Act + Assert
		mockMvc.perform(asAdmin(put(BASE_PATH + "/" + id)).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value(newName))
			.andExpect(jsonPath("$.cost").value(150))
			.andExpect(jsonPath("$.purchaseLimit").value(5))
			.andExpect(jsonPath("$.active").value(true))
			.andExpect(jsonPath("$.cardCount").value(5))
			.andExpect(jsonPath("$.slots[*].rarity.id").value(contains(common.getId(), rare.getId())))
			.andExpect(jsonPath("$.bonuses").isEmpty());
	}

	@Test
	@DisplayName("Actions on a pack that does not exist return 404")
	void actions_unknownPack_return404() throws Exception {
		mockMvc.perform(asAdmin(post(BASE_PATH + "/" + Integer.MAX_VALUE + "/activate")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PACK_NOT_FOUND"));
		mockMvc.perform(asAdmin(put(BASE_PATH + "/" + Integer.MAX_VALUE)).contentType(MediaType.APPLICATION_JSON)
				.content(validBody(uniqueName())))
			.andExpect(status().isNotFound());
	}

	private String validBody(String name) {
		return body(name, 100, null, "[" + slot(common, 3) + ", " + slot(uncommon, 2) + ", " + slot(rare, 2) + "]",
			"[" + bonus(mythic, 1000) + "]");
	}

	private static String body(String name, int cost, Integer limit, String slots, String bonuses) {
		return "{\"name\": \"%s\", \"cost\": %d, \"purchaseLimit\": %s, \"imagePath\": \"packs/test.webp\", \"slots\": %s, \"bonuses\": %s}"
			.formatted(name, cost, limit, slots, bonuses);
	}

	private static String slots(Rarity rarity, int quantity) {
		return "[" + slot(rarity, quantity) + "]";
	}

	private static String slot(Rarity rarity, int quantity) {
		return "{\"rarityId\": %d, \"quantity\": %d}".formatted(rarity.getId(), quantity);
	}

	private static String bonus(Rarity rarity, int units) {
		return "{\"rarityId\": %d, \"probabilityUnits\": %d}".formatted(rarity.getId(), units);
	}

	private static String uniqueName() {
		return TestCards.uniqueName("Pack");
	}

	private ResultActions create(String json) throws Exception {
		return mockMvc.perform(asAdmin(post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private Integer createdId(String json) throws Exception {
		String response = create(json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.createAdmin()));
	}

	private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.create()));
	}
}
