package it.downfall.reversky.api.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
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
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.entity.Faction;
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
 * Tag delle carte (API-12, docs/backoffice e docs/collezione): {@code PUT /admin/cards/{id}/tags}, i tag propri nel
 * dettaglio e nel backoffice, e il filtro {@code tag} di {@code GET /cards}, {@code GET /collection},
 * {@code GET /users/{id}/collection} e {@code GET /admin/cards}, che vale anche per i tag delle fazioni ma mai per
 * Spell e Hidden, che non hanno fazione. Ogni test usa tag, carte e fazioni suoi.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class CardTagsIT {

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
	private CollectionService collectionService;

	@Autowired
	private TransactionTemplate transactionTemplate;

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

	// --- tag propri ---

	@Test
	@DisplayName("Without a token replacing a card's tags returns 401, with a USER token 403")
	void replaceTags_withoutTokenOrAsUser_rejected() throws Exception {
		CatalogCard card = testCards.createCreature();

		mockMvc.perform(put(tagsPath(card)).contentType(MediaType.APPLICATION_JSON).content(body(List.of())))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(as(testUsers.create(), put(tagsPath(card))).contentType(MediaType.APPLICATION_JSON)
				.content(body(List.of())))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	@DisplayName("Setting, replacing and clearing a card's tags: 200 with its own tags sorted by name, saved")
	void replaceTags_setReplaceClear() throws Exception {
		// Arrange
		CatalogCard card = testCards.createCreature();
		int later = tag("ZZ_" + suffix);
		int earlier = tag("AA_" + suffix);

		// Act + Assert
		replaceTags(card, List.of(later, earlier, later))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(card.getId()))
			.andExpect(jsonPath("$.tags[*].name").value(contains("AA_" + suffix, "ZZ_" + suffix)));
		assertThat(savedTagIds(card)).containsExactlyInAnyOrder(earlier, later);

		replaceTags(card, List.of(later))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tags[*].id").value(contains(later)));
		replaceTags(card, List.of())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tags").value(empty()));
		assertThat(savedTagIds(card)).isEmpty();
	}

	@Test
	@DisplayName("An unknown tag is 422 CARD_TAG_INVALID and nothing changes; unknown card 404; bad body 400")
	void replaceTags_errors() throws Exception {
		// Arrange
		CatalogCard card = testCards.createCreature();
		int kept = tag("KEPT_" + suffix);
		replaceTags(card, List.of(kept)).andExpect(status().isOk());

		// Act + Assert
		replaceTags(card, List.of(kept, 999_999_999))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("CARD_TAG_INVALID"))
			.andExpect(jsonPath("$.detail").value("These tags do not exist: 999999999"));
		assertThat(savedTagIds(card)).containsExactly(kept);
		mockMvc.perform(as(admin, put("/api/v1/admin/cards/999999999/tags")).contentType(MediaType.APPLICATION_JSON)
				.content(body(List.of())))
			.andExpect(status().isNotFound());
		mockMvc.perform(as(admin, put(tagsPath(card))).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Tags are not copied to variants: original and variant each keep their own")
	void replaceTags_variantKeepsOwnTags() throws Exception {
		// Arrange
		CatalogCard original = testCards.createCreature();
		CatalogCard variant = testCards.createVariantOf(original, "Event " + suffix);
		int base = tag("BASE_" + suffix);
		int event = tag("EVENT_" + suffix);

		// Act
		replaceTags(variant, List.of(event)).andExpect(status().isOk());
		replaceTags(original, List.of(base)).andExpect(status().isOk());

		// Assert
		assertThat(savedTagIds(original)).containsExactly(base);
		assertThat(savedTagIds(variant)).containsExactly(event);
	}

	@Test
	@DisplayName("Own tags are in GET /cards/{id} and in the admin list, not in the public list")
	void tags_inDetailAndAdminOnly() throws Exception {
		// Arrange
		CatalogCard card = testCards.createCreature();
		replaceTags(card, List.of(tag("SHOWN_" + suffix))).andExpect(status().isOk());

		// Act + Assert
		mockMvc.perform(get("/api/v1/cards/" + card.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tags[*].name").value(contains("SHOWN_" + suffix)));
		mockMvc.perform(as(admin, get("/api/v1/admin/cards").param("name", card.getName())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].tags[*].name").value(contains("SHOWN_" + suffix)));
		mockMvc.perform(get("/api/v1/cards").param("name", card.getName()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0]").value(not(hasKey("tags"))));
	}

	// --- filtro ---

	@Test
	@DisplayName("?tag finds cards by own tag and by the tag of their first or second faction")
	void filter_ownAndFactionTags() throws Exception {
		// Arrange: un tag proprio, uno sulla prima fazione di una carta e lo stesso sulla seconda fazione di un'altra
		String own = "OWN_" + suffix;
		String ofFaction = "FACTION_" + suffix;
		CatalogCard ownTagged = testCards.createCreature();
		replaceTags(ownTagged, List.of(tag(own))).andExpect(status().isOk());
		Faction tagged = testCards.createFaction();
		factionTags(tagged, List.of(tag(ofFaction)));
		CatalogCard first = creatureOf(tagged, null);
		CatalogCard second = creatureOf(testCards.createFaction(), tagged);
		CatalogCard untagged = testCards.createCreature();

		// Act + Assert
		assertThat(publicIds(own)).containsExactly(ownTagged.getId());
		assertThat(publicIds(ofFaction)).containsExactlyInAnyOrder(first.getId(), second.getId());
		assertThat(publicIds(ofFaction)).doesNotContain(untagged.getId(), ownTagged.getId());
	}

	@Test
	@DisplayName("Faction tags never reach SPELL and HIDDEN cards (no faction); their own tags do")
	void filter_spellAndHiddenOnlyOwnTags() throws Exception {
		// Arrange: il tag è sulla fazione delle creature di prova e, come tag proprio, su una sola Spell
		String name = "SHARED_" + suffix;
		int shared = tag(name);
		factionTags(testCards.faction(), List.of(shared));
		CatalogCard creature = testCards.createCreature();
		CatalogCard spell = testCards.create(TestCards.uniqueName("Spell"), CardType.SPELL);
		CatalogCard hidden = testCards.create(TestCards.uniqueName("Hidden"), CardType.HIDDEN);
		CatalogCard ownSpell = testCards.create(TestCards.uniqueName("Spell"), CardType.SPELL);
		replaceTags(ownSpell, List.of(shared)).andExpect(status().isOk());

		// Act
		List<Integer> ids = publicIds(name);

		// Assert
		assertThat(ids).contains(creature.getId(), ownSpell.getId());
		assertThat(ids).doesNotContain(spell.getId(), hidden.getId());
	}

	@Test
	@DisplayName("An unknown tag gives an empty page; the name is compared exactly (lowercase finds nothing)")
	void filter_unknownOrLowercase_empty() throws Exception {
		CatalogCard card = testCards.createCreature();
		replaceTags(card, List.of(tag("EXACT_" + suffix))).andExpect(status().isOk());

		assertThat(publicIds("MISSING_" + suffix)).isEmpty();
		assertThat(publicIds(("EXACT_" + suffix).toLowerCase(Locale.ROOT))).isEmpty();
	}

	@Test
	@DisplayName("The same filter works on GET /collection, GET /users/{id}/collection and GET /admin/cards")
	void filter_collectionAndAdmin() throws Exception {
		// Arrange: due carte nella collezione, una sola con il tag (dalla sua fazione)
		String name = "COLL_" + suffix;
		Faction tagged = testCards.createFaction();
		factionTags(tagged, List.of(tag(name)));
		CatalogCard match = creatureOf(tagged, null);
		CatalogCard other = testCards.createCreature();
		User owner = testUsers.create();
		TestCollections collections = new TestCollections(collectionService, transactionTemplate, jdbcTemplate);
		collections.give(owner, match, 1);
		collections.give(owner, other, 1);

		// Act + Assert
		mockMvc.perform(as(owner, get("/api/v1/collection").param("tag", name)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].card.id").value(contains(match.getId())));
		mockMvc.perform(as(testUsers.create(), get("/api/v1/users/" + owner.getId() + "/collection").param("tag", name)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].card.id").value(contains(match.getId())));
		mockMvc.perform(as(admin, get("/api/v1/admin/cards").param("tag", name)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id").value(containsInAnyOrder(match.getId())));
	}

	private CatalogCard creatureOf(Faction faction, Faction secondary) {
		CatalogCard card = testCards.createCreature();
		card.changeFactions(faction, secondary);
		return catalogCardRepository.save(card);
	}

	private ResultActions replaceTags(CatalogCard card, List<Integer> tagIds) throws Exception {
		return mockMvc.perform(as(admin, put(tagsPath(card))).contentType(MediaType.APPLICATION_JSON).content(body(tagIds)));
	}

	private void factionTags(Faction faction, List<Integer> tagIds) throws Exception {
		mockMvc.perform(as(admin, put("/api/v1/admin/factions/" + faction.getId() + "/tags"))
				.contentType(MediaType.APPLICATION_JSON).content(body(tagIds)))
			.andExpect(status().isOk());
	}

	private int tag(String name) throws Exception {
		String response = mockMvc.perform(as(admin, post("/api/v1/admin/tags")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"" + name + "\"}"))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	// Le carte attive di GET /cards con quel tag, fino a 100
	private List<Integer> publicIds(String tag) throws Exception {
		String response = mockMvc.perform(get("/api/v1/cards").param("tag", tag).param("size", "100"))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.content[*].id");
	}

	private List<Integer> savedTagIds(CatalogCard card) {
		return jdbcTemplate.queryForList("SELECT tag_id FROM card_tags WHERE card_id = ?", Integer.class, card.getId());
	}

	private static String tagsPath(CatalogCard card) {
		return "/api/v1/admin/cards/" + card.getId() + "/tags";
	}

	private static String body(List<Integer> tagIds) {
		return "{\"tagIds\": " + tagIds + "}";
	}

	private MockHttpServletRequestBuilder as(User who, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(who));
	}
}
