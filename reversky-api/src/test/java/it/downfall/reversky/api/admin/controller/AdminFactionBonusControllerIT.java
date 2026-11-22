package it.downfall.reversky.api.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.dto.FactionBonusRequest;
import it.downfall.reversky.api.catalog.dto.AdminFactionResponse;
import it.downfall.reversky.api.catalog.service.FactionBonusService;
import it.downfall.reversky.api.catalog.service.FactionService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestFactionBonuses;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link AdminFactionBonusController} via HTTP, con i filtri di Spring Security attivi.
 * <p>
 * Si verificano accesso (401, 403), status e formato degli errori, compresa la validazione di
 * ogni riga della lista. I casi limite delle regole (matrice invariata dopo un errore, concorrenza)
 * sono in FactionBonusServiceIT. La matrice è globale: si ricorda prima di ogni test e si rimette dopo.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class AdminFactionBonusControllerIT {

	private static final String BASE_PATH = "/api/v1/admin/faction-bonuses";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private FactionBonusService factionBonusService;

	@Autowired
	private FactionService factionService;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa
	// il contesto già avviato (e il suo container) invece di crearne uno nuovo.
	@MockitoBean
	private EmailSender emailSender;

	private TestFactionBonuses testFactionBonuses;

	@BeforeEach
	void setUp() {
		testFactionBonuses = new TestFactionBonuses(factionBonusService, factionService);
		testFactionBonuses.rememberMatrix();
	}

	@AfterEach
	void tearDown() {
		testFactionBonuses.restoreMatrix();
	}

	// --- accesso ---

	@Test
	@DisplayName("Without a token the list returns 401")
	void findAll_withoutToken_returns401() throws Exception {
		// Act + Assert
		mockMvc.perform(get(BASE_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("A USER cannot read the matrix")
	void findAll_userRole_returns403() throws Exception {
		// Act + Assert
		mockMvc.perform(asUser(get(BASE_PATH)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	@DisplayName("Without a token the replacement returns 401")
	void replaceAll_withoutToken_returns401() throws Exception {
		// Act + Assert
		mockMvc.perform(put(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content("[]"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("A USER cannot replace the matrix, and nothing changes")
	void replaceAll_userRole_returns403AndKeepsMatrix() throws Exception {
		// Arrange
		AdminFactionResponse a = testFactionBonuses.createFaction();
		AdminFactionResponse b = testFactionBonuses.createFaction();
		factionBonusService.replaceAll(List.of(new FactionBonusRequest(a.id(), b.id(), 2, 1)));
		List<FactionBonusRequest> before = testFactionBonuses.currentMatrix();

		// Act + Assert
		mockMvc.perform(asUser(put(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content("[]"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		assertThat(testFactionBonuses.currentMatrix()).isEqualTo(before);
	}

	// --- GET ---

	@Test
	@DisplayName("An ADMIN gets the matrix with the faction names")
	void findAll_adminRole_returnsRowsWithNames() throws Exception {
		// Arrange
		AdminFactionResponse a = testFactionBonuses.createFaction();
		AdminFactionResponse b = testFactionBonuses.createFaction();
		factionBonusService.replaceAll(List.of(new FactionBonusRequest(a.id(), b.id(), -1, 2)));

		// Act + Assert
		mockMvc.perform(asAdmin(get(BASE_PATH)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].originFactionId").value(a.id()))
			.andExpect(jsonPath("$[0].originFactionName").value(a.name()))
			.andExpect(jsonPath("$[0].targetFactionId").value(b.id()))
			.andExpect(jsonPath("$[0].targetFactionName").value(b.name()))
			.andExpect(jsonPath("$[0].atkBonus").value(-1))
			.andExpect(jsonPath("$[0].defBonus").value(2));
	}

	// --- PUT valido ---

	@Test
	@DisplayName("An ADMIN replaces the matrix: 200 with the new rows sorted by origin and target")
	void replaceAll_validRows_returns200Sorted() throws Exception {
		// Arrange: le fazioni create dopo hanno id più grandi
		AdminFactionResponse a = testFactionBonuses.createFaction();
		AdminFactionResponse b = testFactionBonuses.createFaction();
		String body = TestFactionBonuses.toJson(List.of(
			new FactionBonusRequest(b.id(), a.id(), -1, 0),
			new FactionBonusRequest(a.id(), b.id(), 2, 1)));

		// Act + Assert
		putMatrix(body)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].originFactionId").value(a.id()))
			.andExpect(jsonPath("$[0].targetFactionName").value(b.name()))
			.andExpect(jsonPath("$[0].atkBonus").value(2))
			.andExpect(jsonPath("$[1].originFactionId").value(b.id()))
			.andExpect(jsonPath("$[1].atkBonus").value(-1));
		assertThat(testFactionBonuses.currentMatrix()).containsExactly(
			new FactionBonusRequest(a.id(), b.id(), 2, 1),
			new FactionBonusRequest(b.id(), a.id(), -1, 0));
	}

	@Test
	@DisplayName("An empty body [] clears the matrix")
	void replaceAll_emptyList_clearsMatrix() throws Exception {
		// Arrange
		AdminFactionResponse a = testFactionBonuses.createFaction();
		AdminFactionResponse b = testFactionBonuses.createFaction();
		factionBonusService.replaceAll(List.of(new FactionBonusRequest(a.id(), b.id(), 2, 1)));

		// Act + Assert
		putMatrix("[]")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
		assertThat(testFactionBonuses.currentMatrix()).isEmpty();
	}

	// --- 400: validazione delle righe ---

	@Test
	@DisplayName("A negative DEF bonus returns 400 with the row index in the field")
	void replaceAll_negativeDef_returns400WithIndexedField() throws Exception {
		// Arrange
		AdminFactionResponse a = testFactionBonuses.createFaction();
		AdminFactionResponse b = testFactionBonuses.createFaction();
		String body = TestFactionBonuses.toJson(List.of(
			new FactionBonusRequest(a.id(), b.id(), 1, 0),
			new FactionBonusRequest(b.id(), a.id(), 1, -1)));

		// Act + Assert
		putMatrix(body)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItem("[1].defBonus")));
	}

	@Test
	@DisplayName("A missing field returns 400 with the row index in the field")
	void replaceAll_missingField_returns400WithIndexedField() throws Exception {
		// Arrange
		AdminFactionResponse a = testFactionBonuses.createFaction();
		AdminFactionResponse b = testFactionBonuses.createFaction();
		String body = "[{\"originFactionId\": " + a.id() + ", \"targetFactionId\": " + b.id() + ", \"defBonus\": 0}]";

		// Act + Assert
		putMatrix(body)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItem("[0].atkBonus")));
	}

	@Test
	@DisplayName("A null row returns 400, not 500")
	void replaceAll_nullRow_returns400() throws Exception {
		// Act + Assert
		putMatrix("[null]")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors", hasSize(1)));
	}

	// --- 422: regole ---

	@Test
	@DisplayName("A faction matched against itself returns 422")
	void replaceAll_selfMatchup_returns422() throws Exception {
		// Arrange
		AdminFactionResponse a = testFactionBonuses.createFaction();

		// Act + Assert
		putMatrix(TestFactionBonuses.toJson(List.of(new FactionBonusRequest(a.id(), a.id(), 1, 0))))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("FACTION_BONUS_SELF_MATCHUP"));
	}

	@Test
	@DisplayName("An unknown faction returns 422")
	void replaceAll_unknownFaction_returns422() throws Exception {
		// Arrange
		AdminFactionResponse a = testFactionBonuses.createFaction();

		// Act + Assert
		putMatrix(TestFactionBonuses.toJson(List.of(new FactionBonusRequest(a.id(), Integer.MAX_VALUE, 1, 0))))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("FACTION_BONUS_FACTION_NOT_FOUND"));
	}

	@Test
	@DisplayName("The same pair twice returns 422")
	void replaceAll_duplicatePair_returns422() throws Exception {
		// Arrange
		AdminFactionResponse a = testFactionBonuses.createFaction();
		AdminFactionResponse b = testFactionBonuses.createFaction();

		// Act + Assert
		putMatrix(TestFactionBonuses.toJson(List.of(
				new FactionBonusRequest(a.id(), b.id(), 1, 0),
				new FactionBonusRequest(a.id(), b.id(), 2, 0))))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("FACTION_BONUS_DUPLICATE"));
	}

	// --- documentazione OpenAPI ---

	@Test
	@DisplayName("OpenAPI requires the Bearer token on both endpoints")
	void apiDocs_factionBonuses_requireBearerToken() throws Exception {
		// Act + Assert
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/admin/faction-bonuses'].get.security[0]").value(hasKey("bearerAuth")))
			.andExpect(jsonPath("$.paths['/api/v1/admin/faction-bonuses'].put.security[0]").value(hasKey("bearerAuth")));
	}

	// --- supporto ---

	private ResultActions putMatrix(String body) throws Exception {
		return mockMvc.perform(asAdmin(put(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.createAdmin()));
	}

	private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.create()));
	}
}
