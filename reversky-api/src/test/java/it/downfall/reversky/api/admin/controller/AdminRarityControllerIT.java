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

import java.util.concurrent.ThreadLocalRandom;

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
import it.downfall.reversky.api.catalog.dto.RarityCreateRequest;
import it.downfall.reversky.api.catalog.dto.RarityResponse;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.catalog.service.RarityService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link AdminRarityController} via HTTP, con i filtri di Spring Security attivi.
 * <p>
 * Si verificano accesso (401, 403), validazione dei limiti, status e formato degli errori, e che
 * sortingOrder non cambi neanche se il client lo invia nel PUT. I casi limite delle regole
 * (stesso nome sulla stessa rarità, 404 prima del 409) sono già in RarityServiceIT.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class AdminRarityControllerIT {

	private static final String BASE_PATH = "/api/v1/admin/rarities";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private RarityService rarityService;

	@Autowired
	private RarityRepository rarityRepository;

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
	@DisplayName("A USER cannot create a rarity, and nothing is saved")
	void create_userRole_returns403AndSavesNothing() throws Exception {
		// Arrange
		String name = uniqueName();

		// Act + Assert
		mockMvc.perform(asUser(post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content(createBody(name, 2, null, uniqueSortingOrder())))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		assertThat(rarityRepository.existsByName(name)).isFalse();
	}

	@Test
	@DisplayName("A USER cannot update a rarity")
	void update_userRole_returns403() throws Exception {
		// Arrange
		RarityResponse rarity = createRarity(uniqueName());

		// Act + Assert
		mockMvc.perform(asUser(put(BASE_PATH + "/" + rarity.id())).contentType(MediaType.APPLICATION_JSON).content(updateBody(uniqueName(), 1, null)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		assertThat(rarityRepository.findById(rarity.id()).orElseThrow().getName()).isEqualTo(rarity.name());
	}

	// --- GET ---

	@Test
	@DisplayName("An ADMIN gets the list of rarities")
	void findAll_adminRole_returnsRarities() throws Exception {
		// Arrange
		RarityResponse rarity = createRarity(uniqueName());

		// Act + Assert
		mockMvc.perform(asAdmin(get(BASE_PATH)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", hasItem(rarity.id())));
	}

	// --- POST ---

	@Test
	@DisplayName("An ADMIN creates a rarity: 201 with the saved rarity")
	void create_validRequest_returns201WithRarity() throws Exception {
		// Arrange
		String name = uniqueName();
		int sortingOrder = uniqueSortingOrder();

		// Act + Assert
		postRarity(createBody(name, 2, 5, sortingOrder))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.name").value(name))
			.andExpect(jsonPath("$.deckLimit").value(2))
			.andExpect(jsonPath("$.maxPerDeck").value(5))
			.andExpect(jsonPath("$.maxAtk").doesNotExist())
			.andExpect(jsonPath("$.maxDef").doesNotExist())
			.andExpect(jsonPath("$.sortingOrder").value(sortingOrder));
		assertThat(rarityRepository.existsByName(name)).isTrue();
	}

	@Test
	@DisplayName("Missing name, deck limit and sorting order return 400 with the three fields")
	void create_missingRequiredFields_returns400WithFields() throws Exception {
		// Act + Assert: maxPerDeck vuoto invece è valido
		postRarity(createBody("", null, null, null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItems("name", "deckLimit", "sortingOrder")));
	}

	@Test
	@DisplayName("Values below the minimums return 400 with every wrong field")
	void create_valuesBelowMinimum_returns400WithFields() throws Exception {
		// Act + Assert: deckLimit >= 1, maxPerDeck >= 0, sortingOrder > 0
		postRarity(createBody(uniqueName(), 0, -1, 0))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItems("deckLimit", "maxPerDeck", "sortingOrder")));
	}

	@Test
	@DisplayName("The lowest allowed values are accepted")
	void create_minimumValues_returns201() throws Exception {
		// Act + Assert: i limiti del DTO coincidono con i CHECK del database, quindi niente 500
		postRarity(createBody(uniqueName(), 1, 0, uniqueSortingOrder()))
			.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("A name already in use returns 409")
	void create_nameAlreadyUsed_returns409() throws Exception {
		// Arrange
		String name = uniqueName();
		createRarity(name);

		// Act + Assert
		postRarity(createBody(name, 2, null, uniqueSortingOrder()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("RARITY_NAME_TAKEN"));
	}

	@Test
	@DisplayName("A sorting order already in use returns 409")
	void create_sortingOrderAlreadyUsed_returns409() throws Exception {
		// Arrange
		RarityResponse existing = createRarity(uniqueName());

		// Act + Assert
		postRarity(createBody(uniqueName(), 2, null, existing.sortingOrder()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("RARITY_SORTING_ORDER_TAKEN"));
	}

	// --- PUT ---

	@Test
	@DisplayName("An ADMIN updates a rarity: 200 with the new values, saved in the database")
	void update_validRequest_returns200WithRarity() throws Exception {
		// Arrange
		RarityResponse rarity = createRarity(uniqueName());
		String newName = uniqueName();

		// Act + Assert
		putRarity(rarity.id(), updateBody(newName, 1, 3))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(rarity.id()))
			.andExpect(jsonPath("$.name").value(newName))
			.andExpect(jsonPath("$.deckLimit").value(1))
			.andExpect(jsonPath("$.maxPerDeck").value(3))
			.andExpect(jsonPath("$.sortingOrder").value(rarity.sortingOrder()));
		Rarity saved = rarityRepository.findById(rarity.id()).orElseThrow();
		assertThat(saved.getName()).isEqualTo(newName);
		assertThat(saved.getDeckLimit()).isEqualTo(1);
	}

	@Test
	@DisplayName("A sorting order sent in the PUT body is ignored")
	void update_withSortingOrderInBody_keepsSortingOrder() throws Exception {
		// Arrange: RarityUpdateRequest non ha il campo, quindi Jackson lo scarta
		RarityResponse rarity = createRarity(uniqueName());
		String body = """
			{"name": "%s", "deckLimit": 2, "sortingOrder": %d}
			""".formatted(uniqueName(), uniqueSortingOrder());

		// Act + Assert
		putRarity(rarity.id(), body)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sortingOrder").value(rarity.sortingOrder()));
		assertThat(rarityRepository.findById(rarity.id()).orElseThrow().getSortingOrder()).isEqualTo(rarity.sortingOrder());
	}

	@Test
	@DisplayName("Updating with invalid values returns 400")
	void update_invalidValues_returns400() throws Exception {
		// Arrange
		RarityResponse rarity = createRarity(uniqueName());

		// Act + Assert
		putRarity(rarity.id(), updateBody("", 0, -1))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItems("name", "deckLimit", "maxPerDeck")));
	}

	@Test
	@DisplayName("Updating a rarity that does not exist returns 404")
	void update_unknownId_returns404() throws Exception {
		// Act + Assert
		putRarity(Integer.MAX_VALUE, updateBody(uniqueName(), 2, null))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("RARITY_NOT_FOUND"));
	}

	@Test
	@DisplayName("Updating with the name of another rarity returns 409")
	void update_nameOfAnotherRarity_returns409() throws Exception {
		// Arrange
		String takenName = uniqueName();
		createRarity(takenName);
		RarityResponse other = createRarity(uniqueName());

		// Act + Assert
		putRarity(other.id(), updateBody(takenName, 2, null))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("RARITY_NAME_TAKEN"));
	}

	// --- documentazione OpenAPI ---

	@Test
	@DisplayName("OpenAPI requires the Bearer token on every admin rarity endpoint")
	void apiDocs_adminRarities_requireBearerToken() throws Exception {
		// Act + Assert: @SecurityRequirement è sulla classe, quindi vale per tutti e tre i metodi
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/admin/rarities'].get.security[0]").value(hasKey("bearerAuth")))
			.andExpect(jsonPath("$.paths['/api/v1/admin/rarities'].post.security[0]").value(hasKey("bearerAuth")))
			.andExpect(jsonPath("$.paths['/api/v1/admin/rarities/{id}'].put.security[0]").value(hasKey("bearerAuth")));
	}

	@Test
	@DisplayName("OpenAPI shows sortingOrder only in the create form")
	void apiDocs_rarityRequests_sortingOrderOnlyOnCreate() throws Exception {
		// Act + Assert: il frontend generato dallo schema non offre un campo che non si può modificare
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.components.schemas.RarityCreateRequest.properties.sortingOrder").exists())
			.andExpect(jsonPath("$.components.schemas.RarityUpdateRequest.properties.name").exists())
			.andExpect(jsonPath("$.components.schemas.RarityUpdateRequest.properties.sortingOrder").doesNotExist());
	}

	// --- supporto ---

	private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.createAdmin()));
	}

	private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.create()));
	}

	private ResultActions postRarity(String json) throws Exception {
		return mockMvc.perform(asAdmin(post(BASE_PATH)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions putRarity(Integer id, String json) throws Exception {
		return mockMvc.perform(asAdmin(put(BASE_PATH + "/" + id)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private RarityResponse createRarity(String name) {
		return rarityService.create(new RarityCreateRequest(name, 2, null, uniqueSortingOrder()));
	}

	private static String uniqueName() {
		return "Rarity " + uniqueSuffix();
	}

	// Valori alti e casuali: non si scontrano con le rarità vere (1-6) né, in pratica, tra loro
	private static int uniqueSortingOrder() {
		return ThreadLocalRandom.current().nextInt(1_000, 1_000_000_000);
	}

	// I null diventano il JSON null, come li manderebbe il frontend per "nessun limite"
	private static String createBody(String name, Integer deckLimit, Integer maxPerDeck, Integer sortingOrder) {
		return """
			{"name": "%s", "deckLimit": %s, "maxPerDeck": %s, "sortingOrder": %s}
			""".formatted(name, deckLimit, maxPerDeck, sortingOrder);
	}

	private static String updateBody(String name, Integer deckLimit, Integer maxPerDeck) {
		return """
			{"name": "%s", "deckLimit": %s, "maxPerDeck": %s}
			""".formatted(name, deckLimit, maxPerDeck);
	}
}
