package it.downfall.reversky.api.pack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Locale;

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

import com.jayway.jsonpath.JsonPath;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.entity.Faction;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Tag dei pacchetti (API-21, docs/pacchetti e docs/backoffice): {@code tagIds} in POST e PUT /admin/packs, il pacchetto
 * attivo che deve poter riempire ogni slot con i suoi tag, e un'apertura vera che dà solo carte con quei tag. Le regole
 * fini del filtro (varianti, Spell e Hidden) sono in CardPoolTagsTest. Ogni test usa tag, fazioni e carte suoi.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class PackTagsIT {

	private static final String ADMIN_PACKS = "/api/v1/admin/packs";

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

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	private TestCards testCards;
	private User admin;
	private String suffix;

	@BeforeEach
	void setUp() {
		testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
		admin = testUsers.createAdmin();
		suffix = TestUsers.uniqueSuffix().toUpperCase(Locale.ROOT);
	}

	@Test
	@DisplayName("A pack is created and updated with tagIds; the admin responses show the tags; no tagIds = no tags")
	void tagIds_createUpdateAndClear() throws Exception {
		// Arrange
		int later = tag("ZZ_" + suffix);
		int earlier = tag("AA_" + suffix);

		// Act + Assert: creazione con due tag (uno ripetuto), in ordine di nome nella risposta
		int packId = createdId(packBody("Pack " + suffix, List.of(later, earlier, later)));
		mockMvc.perform(as(admin, get(ADMIN_PACKS)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.id == " + packId + ")].tags[*].name")
				.value(contains("AA_" + suffix, "ZZ_" + suffix)));

		// la modifica sostituisce tutto: un tag solo, poi senza tagIds nessun tag
		update(packId, packBody("Pack " + suffix, List.of(earlier)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tags[*].id").value(contains(earlier)));
		update(packId, packBody("Pack " + suffix, null))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tags").value(empty()));
		assertThat(savedTagIds(packId)).isEmpty();
	}

	@Test
	@DisplayName("An unknown tag is 422 PACK_TAG_INVALID: the pack is not created, and an update changes nothing")
	void tagIds_unknownTag_returns422() throws Exception {
		// Arrange
		int kept = tag("KEPT_" + suffix);
		int packId = createdId(packBody("Pack " + suffix, List.of(kept)));

		// Act + Assert
		mockMvc.perform(as(admin, post(ADMIN_PACKS)).contentType(MediaType.APPLICATION_JSON)
				.content(packBody("Other " + suffix, List.of(999_999_999))))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("PACK_TAG_INVALID"));
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM packs WHERE name = ?", Integer.class,
			"Other " + suffix)).isZero();
		update(packId, packBody("Pack " + suffix, List.of(kept, 999_999_999)))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("PACK_TAG_INVALID"));
		assertThat(savedTagIds(packId)).containsExactly(kept);
	}

	@Test
	@DisplayName("A pack whose tags no card has cannot be activated, and an active pack cannot take such a tag: 422")
	void tags_mustFillEverySlot() throws Exception {
		// Arrange: un tag che nessuna carta ha, e un pacchetto attivo senza tag
		int nobody = tag("NOBODY_" + suffix);
		int emptyPack = createdId(packBody("Empty " + suffix, List.of(nobody)));
		int activePack = createdId(packBody("Active " + suffix, null));
		activate(activePack).andExpect(status().isOk());

		// Act + Assert
		activate(emptyPack)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("PACK_NOT_FILLABLE"));
		update(activePack, packBody("Active " + suffix, List.of(nobody)))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("PACK_NOT_FILLABLE"));
		assertThat(savedTagIds(activePack)).isEmpty();
	}

	@Test
	@DisplayName("Opening a tagged pack gives only cards with its tag, here through their faction")
	void open_taggedPack_onlyTaggedCards() throws Exception {
		// Arrange: un tag sulla fazione di due creature Common; pacchetto gratuito da 3 Common con quel tag
		int tagId = tag("EVENT_" + suffix);
		Faction faction = testCards.createFaction();
		mockMvc.perform(as(admin, put("/api/v1/admin/factions/" + faction.getId() + "/tags"))
				.contentType(MediaType.APPLICATION_JSON).content("{\"tagIds\": [" + tagId + "]}"))
			.andExpect(status().isOk());
		CatalogCard first = creatureOf(faction);
		CatalogCard second = creatureOf(faction);
		int packId = createdId(packBody("Event " + suffix, List.of(tagId)));
		activate(packId).andExpect(status().isOk());
		User player = testUsers.create();
		mockMvc.perform(as(player, post("/api/v1/packs/" + packId + "/purchase"))
				.contentType(MediaType.APPLICATION_JSON).content("{\"quantity\": 1}"))
			.andExpect(status().isOk());

		// Act
		String response = mockMvc.perform(as(player, post("/api/v1/inventory/packs/" + packId + "/open")))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();

		// Assert
		List<Integer> cardIds = JsonPath.read(response, "$.cards[*].card.id");
		assertThat(cardIds).hasSize(3).allMatch(id -> id.equals(first.getId()) || id.equals(second.getId()));
	}

	private CatalogCard creatureOf(Faction faction) {
		CatalogCard card = testCards.createCreature();
		card.changeFactions(faction, null);
		return catalogCardRepository.save(card);
	}

	// Pacchetto gratuito (quindi con un limite) da 3 Common, senza bonus; tagIds null = campo assente
	private String packBody(String name, List<Integer> tagIds) {
		String tags = tagIds == null ? "" : ", \"tagIds\": " + tagIds;
		return "{\"name\": \"" + name + "\", \"cost\": 0, \"purchaseLimit\": 3, \"imagePath\": \"packs/test.webp\", "
			+ "\"slots\": [{\"rarityId\": " + testCards.common().getId() + ", \"quantity\": 3}], \"bonuses\": []" + tags + "}";
	}

	private int createdId(String body) throws Exception {
		String response = mockMvc.perform(as(admin, post(ADMIN_PACKS)).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private ResultActions update(int packId, String body) throws Exception {
		return mockMvc.perform(as(admin, put(ADMIN_PACKS + "/" + packId)).contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private ResultActions activate(int packId) throws Exception {
		return mockMvc.perform(as(admin, post(ADMIN_PACKS + "/" + packId + "/activate")));
	}

	private int tag(String name) throws Exception {
		String response = mockMvc.perform(as(admin, post("/api/v1/admin/tags")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"" + name + "\"}"))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private List<Integer> savedTagIds(int packId) {
		return jdbcTemplate.queryForList("SELECT tag_id FROM pack_tags WHERE pack_id = ?", Integer.class, packId);
	}

	private MockHttpServletRequestBuilder as(User who, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(who));
	}
}
