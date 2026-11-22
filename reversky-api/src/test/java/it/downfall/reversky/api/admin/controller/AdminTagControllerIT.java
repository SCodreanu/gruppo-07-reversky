package it.downfall.reversky.api.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * {@link AdminTagController} via HTTP (API-07, docs/backoffice): elenco in ordine di nome, creazione, rinomina, nome
 * univoco e formato {@code ^[A-Z0-9_]+$} controllato così com'è.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class AdminTagControllerIT {

	private static final String BASE_PATH = "/api/v1/admin/tags";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	private User admin;

	@BeforeEach
	void setUp() {
		admin = testUsers.createAdmin();
	}

	@Test
	@DisplayName("Without a token the tags return 401, with a USER token 403")
	void tags_withoutTokenOrAsUser_rejected() throws Exception {
		mockMvc.perform(get(BASE_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
		mockMvc.perform(as(testUsers.create(), get(BASE_PATH)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	@DisplayName("Creating a tag returns 201 with id and name; the list has it, sorted by name")
	void create_thenListSortedByName() throws Exception {
		// Arrange: due nomi che in ordine alfabetico vanno al contrario dell'ordine di creazione
		String suffix = suffix();
		String later = "ZZ_" + suffix;
		String earlier = "AA_" + suffix;

		// Act
		create(later)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.name").value(later));
		create(earlier).andExpect(status().isCreated());

		// Assert
		String body = mockMvc.perform(as(admin, get(BASE_PATH)))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		List<String> names = JsonPath.read(body, "$[*].name");
		// Solo l'ordine relativo dei due nomi: l'ordine completo dipende dalla collation del database
		assertThat(names).contains(earlier, later);
		assertThat(names.indexOf(earlier)).isLessThan(names.indexOf(later));
	}

	@Test
	@DisplayName("A name already used returns 409 TAG_NAME_TAKEN and creates nothing")
	void create_duplicate_returns409() throws Exception {
		// Arrange
		String name = "DUP_" + suffix();
		create(name).andExpect(status().isCreated());

		// Act + Assert
		create(name)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("TAG_NAME_TAKEN"));
		assertThat(rowsNamed(name)).isEqualTo(1);
	}

	@Test
	@DisplayName("Lowercase, spaces, other characters, blank and 101 characters return 400; nothing is normalized")
	void create_wrongFormat_returns400() throws Exception {
		// Arrange: deciso il 22 novembre, il nome non si toglie degli spazi né si rende maiuscolo
		String lowercase = "dream_" + suffix().toLowerCase(Locale.ROOT);
		List<String> wrong = List.of(lowercase, " DREAM ", "CHRISTMAS 2026", "EVENT-1", "", "A".repeat(101));

		// Act + Assert
		for (String name : wrong) {
			create(name).andExpect(status().isBadRequest());
		}
		mockMvc.perform(as(admin, post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest());
		assertThat(rowsNamed(lowercase.toUpperCase(Locale.ROOT))).isZero();
	}

	@Test
	@DisplayName("100 characters in the format are accepted")
	void create_maxLength_accepted() throws Exception {
		String name = suffix() + "_" + "X".repeat(91);

		create(name)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value(name));
	}

	@Test
	@DisplayName("Renaming returns 200; keeping its own name is fine; another tag's name is 409")
	void update_renameAndConflicts() throws Exception {
		// Arrange
		String suffix = suffix();
		int id = idOf(create("OLD_" + suffix));
		create("TAKEN_" + suffix).andExpect(status().isCreated());

		// Act + Assert
		update(id, "NEW_" + suffix)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id))
			.andExpect(jsonPath("$.name").value("NEW_" + suffix));
		update(id, "NEW_" + suffix).andExpect(status().isOk());
		update(id, "TAKEN_" + suffix)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("TAG_NAME_TAKEN"));
		assertThat(jdbcTemplate.queryForObject("SELECT name FROM tags WHERE id = ?", String.class, id))
			.isEqualTo("NEW_" + suffix);
	}

	@Test
	@DisplayName("Renaming a tag that does not exist returns 404 TAG_NOT_FOUND; a wrong name is still 400 first")
	void update_missingOrWrongFormat() throws Exception {
		update(999_999_999, "GHOST_" + suffix())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TAG_NOT_FOUND"));
		update(999_999_999, "ghost")
			.andExpect(status().isBadRequest());
	}

	private ResultActions create(String name) throws Exception {
		return mockMvc.perform(as(admin, post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content(body(name)));
	}

	private ResultActions update(int id, String name) throws Exception {
		return mockMvc.perform(as(admin, put(BASE_PATH + "/" + id)).contentType(MediaType.APPLICATION_JSON)
			.content(body(name)));
	}

	private int idOf(ResultActions created) throws Exception {
		return JsonPath.read(created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
			"$.id");
	}

	private int rowsNamed(String name) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM tags WHERE name = ?", Integer.class, name);
	}

	// I tag restano nel database tra un test e l'altro: ogni test usa nomi suoi
	private static String suffix() {
		return TestUsers.uniqueSuffix().toUpperCase(Locale.ROOT);
	}

	private static String body(String name) {
		return "{\"name\": \"" + name + "\"}";
	}

	private MockHttpServletRequestBuilder as(User who, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(who));
	}
}
