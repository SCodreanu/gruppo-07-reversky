package it.downfall.reversky.api.admin.controller;

import static it.downfall.reversky.api.support.TestUsers.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import it.downfall.reversky.api.catalog.dto.FactionRequest;
import it.downfall.reversky.api.catalog.dto.FactionResponse;
import it.downfall.reversky.api.catalog.entity.Faction;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.service.FactionService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link AdminFactionController} via HTTP, con i filtri di Spring Security attivi.
 * <p>
 * Si verificano accesso (401, 403), status e formato degli errori. I casi limite delle regole
 * (stesso nome sulla stessa fazione, 404 prima del 409) sono già in FactionServiceIT.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class AdminFactionControllerIT {

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
	private FactionRepository factionRepository;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa
	// il contesto già avviato (e il suo container) invece di crearne uno nuovo.
	@MockitoBean
	private EmailSender emailSender;

	// --- accesso ---

	@Test
	@DisplayName("Without a token the admin list returns 401")
	void findAll_withoutToken_returns401() throws Exception {
		// Act + Assert
		mockMvc.perform(get(BASE_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("A USER cannot read the admin list")
	void findAll_userRole_returns403() throws Exception {
		// Act + Assert
		mockMvc.perform(asUser(get(BASE_PATH)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	@DisplayName("A USER cannot create a faction, and nothing is saved")
	void create_userRole_returns403AndSavesNothing() throws Exception {
		// Arrange
		String name = uniqueName();

		// Act + Assert
		mockMvc.perform(asUser(post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content(body(name, "factions/test.svg", null)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		assertThat(factionRepository.existsByName(name)).isFalse();
	}

	@Test
	@DisplayName("A USER cannot update a faction")
	void update_userRole_returns403() throws Exception {
		// Arrange
		FactionResponse faction = createFaction(uniqueName());

		// Act + Assert
		mockMvc.perform(asUser(put(BASE_PATH + "/" + faction.id())).contentType(MediaType.APPLICATION_JSON).content(body(uniqueName(), "factions/test.svg", null)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		assertThat(factionRepository.findById(faction.id()).orElseThrow().getName()).isEqualTo(faction.name());
	}

	// --- GET ---

	@Test
	@DisplayName("An ADMIN gets the list of factions")
	void findAll_adminRole_returnsFactions() throws Exception {
		// Arrange
		FactionResponse faction = createFaction(uniqueName());

		// Act + Assert
		mockMvc.perform(asAdmin(get(BASE_PATH)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", hasItem(faction.id())));
	}

	// --- POST ---

	@Test
	@DisplayName("An ADMIN creates a faction: 201 with the saved faction")
	void create_validRequest_returns201WithFaction() throws Exception {
		// Arrange
		String name = uniqueName();

		// Act + Assert
		postFaction(body(name, "factions/test.svg", "A test faction"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.name").value(name))
			.andExpect(jsonPath("$.emblemPath").value("factions/test.svg"))
			.andExpect(jsonPath("$.description").value("A test faction"));
		assertThat(factionRepository.existsByName(name)).isTrue();
	}

	@Test
	@DisplayName("Missing name and emblem path return 400 with both fields")
	void create_blankFields_returns400WithFields() throws Exception {
		// Act + Assert
		postFaction(body("", " ", null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItems("name", "emblemPath")));
	}

	@Test
	@DisplayName("A name longer than 100 characters returns 400")
	void create_nameTooLong_returns400() throws Exception {
		// Act + Assert
		postFaction(body("a".repeat(101), "factions/test.svg", null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItem("name")));
	}

	@Test
	@DisplayName("A name already in use returns 409")
	void create_nameAlreadyUsed_returns409() throws Exception {
		// Arrange
		String name = uniqueName();
		createFaction(name);

		// Act + Assert
		postFaction(body(name, "factions/other.svg", null))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("FACTION_NAME_TAKEN"));
	}

	// --- PUT ---

	@Test
	@DisplayName("An ADMIN updates a faction: 200 with the new values, saved in the database")
	void update_validRequest_returns200WithFaction() throws Exception {
		// Arrange
		FactionResponse faction = createFaction(uniqueName());
		String newName = uniqueName();

		// Act + Assert
		putFaction(faction.id(), body(newName, "factions/new.svg", "New text"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(faction.id()))
			.andExpect(jsonPath("$.name").value(newName))
			.andExpect(jsonPath("$.emblemPath").value("factions/new.svg"))
			.andExpect(jsonPath("$.description").value("New text"));
		Faction saved = factionRepository.findById(faction.id()).orElseThrow();
		assertThat(saved.getName()).isEqualTo(newName);
		assertThat(saved.getEmblemPath()).isEqualTo("factions/new.svg");
	}

	@Test
	@DisplayName("Updating with a blank name returns 400")
	void update_blankName_returns400() throws Exception {
		// Arrange
		FactionResponse faction = createFaction(uniqueName());

		// Act + Assert
		putFaction(faction.id(), body("", "factions/test.svg", null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItem("name")));
	}

	@Test
	@DisplayName("Updating a faction that does not exist returns 404")
	void update_unknownId_returns404() throws Exception {
		// Act + Assert
		putFaction(Integer.MAX_VALUE, body(uniqueName(), "factions/test.svg", null))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FACTION_NOT_FOUND"));
	}

	@Test
	@DisplayName("Updating with the name of another faction returns 409")
	void update_nameOfAnotherFaction_returns409() throws Exception {
		// Arrange
		String takenName = uniqueName();
		createFaction(takenName);
		FactionResponse other = createFaction(uniqueName());

		// Act + Assert
		putFaction(other.id(), body(takenName, "factions/test.svg", null))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("FACTION_NAME_TAKEN"));
	}

	// --- documentazione OpenAPI ---

	@Test
	@DisplayName("OpenAPI requires the Bearer token on every admin faction endpoint")
	void apiDocs_adminFactions_requireBearerToken() throws Exception {
		// Act + Assert: @SecurityRequirement è sulla classe, quindi vale per tutti e tre i metodi
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/admin/factions'].get.security[0]").value(hasKey("bearerAuth")))
			.andExpect(jsonPath("$.paths['/api/v1/admin/factions'].post.security[0]").value(hasKey("bearerAuth")))
			.andExpect(jsonPath("$.paths['/api/v1/admin/factions/{id}'].put.security[0]").value(hasKey("bearerAuth")));
	}

	// --- supporto ---

	private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.createAdmin()));
	}

	private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.create()));
	}

	private ResultActions postFaction(String json) throws Exception {
		return mockMvc.perform(asAdmin(post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions putFaction(Integer id, String json) throws Exception {
		return mockMvc.perform(asAdmin(put(BASE_PATH + "/" + id)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private FactionResponse createFaction(String name) {
		return factionService.create(new FactionRequest(name, "factions/test.svg", "A test faction"));
	}

	private static String uniqueName() {
		return "Faction " + uniqueSuffix();
	}

	// description null diventa il JSON null, come la manderebbe il frontend senza descrizione
	private static String body(String name, String emblemPath, String description) {
		String descriptionJson = description == null ? "null" : "\"" + description + "\"";
		return "{\"name\": \"%s\", \"emblemPath\": \"%s\", \"description\": %s}".formatted(name, emblemPath, descriptionJson);
	}
}
