package it.downfall.reversky.api.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.dto.FactionRequest;
import it.downfall.reversky.api.catalog.dto.TagRequest;
import it.downfall.reversky.api.catalog.service.FactionService;
import it.downfall.reversky.api.catalog.service.TagService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * Tag delle fazioni dal backoffice (API-13, docs/backoffice): {@code PUT /admin/factions/{id}/tags} e i tag nelle
 * risposte di {@code /admin/factions}, non in quelle pubbliche. Ogni test usa fazioni e tag suoi, così non tocca le
 * quattro fazioni di V9 e i loro tag di V15 (controllati in FactionTagsMigrationIT).
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class AdminFactionTagsIT {

	private static final String BASE_PATH = "/api/v1/admin/factions";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private FactionService factionService;

	@Autowired
	private TagService tagService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	private User admin;
	private Integer factionId;
	private String suffix;

	@BeforeEach
	void setUp() {
		admin = testUsers.createAdmin();
		suffix = TestUsers.uniqueSuffix().toUpperCase(Locale.ROOT);
		factionId = factionService.create(new FactionRequest("Faction " + suffix, "factions/test.svg", null)).id();
	}

	@Test
	@DisplayName("Without a token replacing tags returns 401, with a USER token 403")
	void replaceTags_withoutTokenOrAsUser_rejected() throws Exception {
		mockMvc.perform(put(tagsPath(factionId)).contentType(MediaType.APPLICATION_JSON).content(body(List.of())))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(as(testUsers.create(), put(tagsPath(factionId))).contentType(MediaType.APPLICATION_JSON)
				.content(body(List.of())))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	@DisplayName("Setting tags returns 200 with the faction and its tags sorted by name; they are saved")
	void replaceTags_setsTags() throws Exception {
		// Arrange
		int later = tag("ZZ_" + suffix);
		int earlier = tag("AA_" + suffix);

		// Act + Assert
		replaceTags(factionId, List.of(later, earlier))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(factionId))
			.andExpect(jsonPath("$.name").value("Faction " + suffix))
			.andExpect(jsonPath("$.tags[*].name").value(contains("AA_" + suffix, "ZZ_" + suffix)))
			.andExpect(jsonPath("$.tags[*].id").value(contains(earlier, later)));
		assertThat(savedTagIds(factionId)).containsExactlyInAnyOrder(earlier, later);
	}

	@Test
	@DisplayName("A second PUT replaces the whole set; an empty list removes every tag")
	void replaceTags_replacesAndClears() throws Exception {
		// Arrange
		int first = tag("FIRST_" + suffix);
		int second = tag("SECOND_" + suffix);
		replaceTags(factionId, List.of(first)).andExpect(status().isOk());

		// Act + Assert
		replaceTags(factionId, List.of(second))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tags[*].id").value(contains(second)));
		assertThat(savedTagIds(factionId)).containsExactly(second);

		replaceTags(factionId, List.of())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tags").value(empty()));
		assertThat(savedTagIds(factionId)).isEmpty();
	}

	@Test
	@DisplayName("A repeated tag id counts once")
	void replaceTags_duplicateIds_countOnce() throws Exception {
		int id = tag("ONCE_" + suffix);

		replaceTags(factionId, List.of(id, id, id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tags[*].id").value(contains(id)));
		assertThat(savedTagIds(factionId)).containsExactly(id);
	}

	@Test
	@DisplayName("An unknown tag returns 422 FACTION_TAG_INVALID naming it, and the tags stay as they were")
	void replaceTags_unknownTag_returns422AndKeepsTags() throws Exception {
		// Arrange
		int kept = tag("KEPT_" + suffix);
		replaceTags(factionId, List.of(kept)).andExpect(status().isOk());

		// Act + Assert
		replaceTags(factionId, List.of(kept, 999_999_999))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("FACTION_TAG_INVALID"))
			.andExpect(jsonPath("$.detail").value("These tags do not exist: 999999999"));
		assertThat(savedTagIds(factionId)).containsExactly(kept);
	}

	@Test
	@DisplayName("An unknown faction returns 404; a missing list or a null id returns 400")
	void replaceTags_missingFactionOrBadBody() throws Exception {
		replaceTags(999_999_999, List.of())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FACTION_NOT_FOUND"));
		mockMvc.perform(as(admin, put(tagsPath(factionId))).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(as(admin, put(tagsPath(factionId))).contentType(MediaType.APPLICATION_JSON)
				.content("{\"tagIds\": [null]}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("The admin list shows the tags; the public list does not have the field at all")
	void tags_inAdminListOnly() throws Exception {
		// Arrange
		int id = tag("SHOWN_" + suffix);
		replaceTags(factionId, List.of(id)).andExpect(status().isOk());

		// Act + Assert
		mockMvc.perform(as(admin, get(BASE_PATH)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.id == " + factionId + ")].tags[*].name").value(contains("SHOWN_" + suffix)));
		mockMvc.perform(get("/api/v1/factions"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0]").value(not(hasKey("tags"))));
	}

	@Test
	@DisplayName("Renaming a faction keeps its tags; a new faction starts without tags")
	void updateFaction_keepsTags() throws Exception {
		// Arrange
		int id = tag("STAY_" + suffix);
		replaceTags(factionId, List.of(id)).andExpect(status().isOk());

		// Act + Assert
		mockMvc.perform(as(admin, put(BASE_PATH + "/" + factionId)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Renamed " + suffix + "\", \"emblemPath\": \"factions/test.svg\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tags[*].id").value(contains(id)));
		assertThat(savedTagIds(factionId)).containsExactly(id);
		assertThat(factionService.create(new FactionRequest("New " + suffix, "factions/test.svg", null)).tags())
			.isEmpty();
	}

	private ResultActions replaceTags(int id, List<Integer> tagIds) throws Exception {
		return mockMvc.perform(as(admin, put(tagsPath(id))).contentType(MediaType.APPLICATION_JSON).content(body(tagIds)));
	}

	private int tag(String name) {
		return tagService.create(new TagRequest(name)).id();
	}

	private List<Integer> savedTagIds(int id) {
		return jdbcTemplate.queryForList("SELECT tag_id FROM faction_tags WHERE faction_id = ?", Integer.class, id);
	}

	private static String tagsPath(int id) {
		return BASE_PATH + "/" + id + "/tags";
	}

	private static String body(List<Integer> tagIds) {
		return "{\"tagIds\": " + tagIds + "}";
	}

	private MockHttpServletRequestBuilder as(User who, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(who));
	}
}
