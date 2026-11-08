package it.downfall.reversky.api.admin.controller;

import static it.downfall.reversky.api.support.TestCards.uniqueName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
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

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.entity.CardSubtype;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link AdminCardController} via HTTP, con i filtri di Spring Security attivi.
 * <p>
 * Si verificano accesso (401, 403), validazione del DTO (400), status e codici degli errori del
 * service, e i "test minimi" di docs/backoffice sulle carte. I casi limite delle regole sono già in
 * CatalogCardServiceIT.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class AdminCardControllerIT {

	private static final String BASE_PATH = "/api/v1/admin/cards";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private CatalogCardRepository catalogCardRepository;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa
	// il contesto già avviato (e il suo container) invece di crearne uno nuovo.
	@MockitoBean
	private EmailSender emailSender;

	private TestCards testCards;

	@BeforeEach
	void setUp() {
		testCards = new TestCards(catalogCardRepository);
	}

	// --- accesso ---

	@Test
	@DisplayName("Without a token the admin card list returns 401")
	void findAll_withoutToken_returns401() throws Exception {
		// Act + Assert
		mockMvc.perform(get(BASE_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("A USER cannot read the admin card list")
	void findAll_userRole_returns403() throws Exception {
		// Act + Assert
		mockMvc.perform(asUser(get(BASE_PATH)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	@DisplayName("A USER cannot update a card, and nothing changes")
	void update_userRole_returns403AndChangesNothing() throws Exception {
		// Arrange
		CatalogCard card = testCards.createCreature();

		// Act + Assert
		mockMvc.perform(asUser(put(BASE_PATH + "/" + card.getId())).contentType(MediaType.APPLICATION_JSON)
				.content(creatureBody(uniqueName("Renamed"), null)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		assertThat(reload(card).getName()).isEqualTo(card.getName());
	}

	@Test
	@DisplayName("A USER cannot deactivate or activate a card")
	void deactivateAndActivate_userRole_return403() throws Exception {
		// Arrange
		CatalogCard card = testCards.createCreature();

		// Act + Assert
		mockMvc.perform(asUser(post(BASE_PATH + "/" + card.getId() + "/deactivate")))
			.andExpect(status().isForbidden());
		mockMvc.perform(asUser(post(BASE_PATH + "/" + card.getId() + "/activate")))
			.andExpect(status().isForbidden());
		assertThat(reload(card).isActive()).isTrue();
	}

	// --- GET ---

	@Test
	@DisplayName("The admin list also shows deactivated cards")
	void findAll_adminRole_includesDeactivatedCards() throws Exception {
		// Arrange: il token nel nome limita la ricerca alle carte di questo test
		String token = TestUsers.uniqueSuffix();
		CatalogCard active = testCards.create("Active " + token, CardType.CREATURE);
		CatalogCard inactive = testCards.deactivate(testCards.create("Inactive " + token, CardType.CREATURE));

		// Act + Assert
		mockMvc.perform(asAdmin(get(BASE_PATH)).param("name", token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id", hasItems(active.getId(), inactive.getId())))
			.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	@DisplayName("The active parameter filters the admin list")
	void findAll_activeFalse_returnsOnlyDeactivated() throws Exception {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		testCards.create("Active " + token, CardType.CREATURE);
		CatalogCard inactive = testCards.deactivate(testCards.create("Inactive " + token, CardType.CREATURE));

		// Act + Assert
		mockMvc.perform(asAdmin(get(BASE_PATH)).param("name", token).param("active", "false"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(inactive.getId()));
	}

	// --- PUT ---

	@Test
	@DisplayName("An ADMIN updates a card: 200 with the new values, saved in the database")
	void update_validRequest_returns200WithCard() throws Exception {
		// Arrange
		CatalogCard card = testCards.createCreature();
		String newName = uniqueName("Renamed");

		// Act + Assert
		putCard(card.getId(), body(newName, CardType.HIDDEN, CardSubtype.DAMAGE, "HIDE_BEHIND", null, null, null, 1))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(card.getId()))
			.andExpect(jsonPath("$.name").value(newName))
			.andExpect(jsonPath("$.type").value("HIDDEN"))
			.andExpect(jsonPath("$.subtype").value("DAMAGE"))
			.andExpect(jsonPath("$.effectCode").value("HIDE_BEHIND"))
			.andExpect(jsonPath("$.atk").isEmpty())
			.andExpect(jsonPath("$.copyLimit").value(1));
		CatalogCard saved = reload(card);
		assertThat(saved.getName()).isEqualTo(newName);
		assertThat(saved.getCardType()).isEqualTo(CardType.HIDDEN);
	}

	@Test
	@DisplayName("Updating an original gives its variants the same game fields")
	void update_originalWithVariant_updatesVariant() throws Exception {
		// Arrange
		CatalogCard original = testCards.createCreature();
		CatalogCard variant = testCards.createVariantOf(original);

		// Act + Assert
		putCard(original.getId(), body(original.getName(), CardType.CREATURE, null, null, null, 13, 14, null))
			.andExpect(status().isOk());
		CatalogCard savedVariant = reload(variant);
		assertThat(savedVariant.getAtk()).isEqualTo(13);
		assertThat(savedVariant.getDef()).isEqualTo(14);
	}

	@Test
	@DisplayName("An image path sent in the PUT body is ignored")
	void update_withImagePathInBody_keepsImagePath() throws Exception {
		// Arrange: CatalogCardUpdateRequest non ha il campo, quindi Jackson lo scarta
		CatalogCard card = testCards.createCreature();
		String bodyWithImage = """
			{"name": "%s", "type": "CREATURE", "atk": 1, "def": 1, "imagePath": "/images/other.webp"}
			""".formatted(card.getName());

		// Act + Assert
		putCard(card.getId(), bodyWithImage)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imagePath").value(TestCards.IMAGE_PATH));
		assertThat(reload(card).getImagePath()).isEqualTo(TestCards.IMAGE_PATH);
	}

	@Test
	@DisplayName("Missing name and type return 400 with both fields")
	void update_missingRequiredFields_returns400WithFields() throws Exception {
		// Arrange
		CatalogCard card = testCards.createCreature();

		// Act + Assert
		putCard(card.getId(), body("", null, null, null, null, 1, 1, null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItems("name", "type")));
	}

	@Test
	@DisplayName("Values below the minimums and a blank effect code return 400 with every wrong field")
	void update_invalidValues_returns400WithFields() throws Exception {
		// Arrange
		CatalogCard card = testCards.createCreature();

		// Act + Assert: atk >= 0, def >= 1, copyLimit >= 1, effectCode non vuoto
		putCard(card.getId(), body(card.getName(), CardType.CREATURE, null, "   ", null, -1, 0, 0))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItems("atk", "def", "copyLimit", "effectCode")));
	}

	@Test
	@DisplayName("An unknown type in the body returns 400")
	void update_unknownType_returns400() throws Exception {
		// Arrange
		CatalogCard card = testCards.createCreature();

		// Act + Assert
		putCard(card.getId(), """
			{"name": "%s", "type": "DRAGON", "atk": 1, "def": 1}
			""".formatted(card.getName()))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Updating a card that does not exist returns 404")
	void update_unknownId_returns404() throws Exception {
		// Act + Assert
		putCard(Integer.MAX_VALUE, creatureBody(uniqueName("Any"), null))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"));
	}

	@Test
	@DisplayName("Using the name of another original returns 409")
	void update_nameOfAnotherOriginal_returns409() throws Exception {
		// Arrange
		CatalogCard card = testCards.createCreature();
		CatalogCard other = testCards.createCreature();

		// Act + Assert
		putCard(card.getId(), creatureBody(other.getName(), null))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CARD_NAME_TAKEN"));
	}

	@Test
	@DisplayName("A spell without a subtype returns 422")
	void update_spellWithoutSubtype_returns422() throws Exception {
		// Arrange
		CatalogCard card = testCards.createCreature();

		// Act + Assert
		putCard(card.getId(), body(card.getName(), CardType.SPELL, null, TestCards.EFFECT_CODE, null, null, null, null))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("CARD_RULES_VIOLATED"));
	}

	@Test
	@DisplayName("A variantOfId that points to a variant returns 422")
	void update_variantOfVariant_returns422() throws Exception {
		// Arrange
		CatalogCard original = testCards.createCreature();
		CatalogCard variant = testCards.createVariantOf(original);
		CatalogCard card = testCards.createCreature();

		// Act + Assert
		putCard(card.getId(), creatureBody(card.getName(), variant.getId()))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("CARD_VARIANT_INVALID"));
		assertThat(reload(card).isVariant()).isFalse();
	}

	// --- deactivate e activate ---

	@Test
	@DisplayName("Deactivating a card returns 200 with active false; the card is not deleted")
	void deactivate_adminRole_returns200() throws Exception {
		// Arrange
		CatalogCard card = testCards.createCreature();

		// Act + Assert
		mockMvc.perform(asAdmin(post(BASE_PATH + "/" + card.getId() + "/deactivate")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(card.getId()))
			.andExpect(jsonPath("$.active").value(false));
		assertThat(reload(card).isActive()).isFalse();
	}

	@Test
	@DisplayName("Activating a deactivated card returns 200 with active true")
	void activate_adminRole_returns200() throws Exception {
		// Arrange
		CatalogCard card = testCards.deactivate(testCards.createCreature());

		// Act + Assert
		mockMvc.perform(asAdmin(post(BASE_PATH + "/" + card.getId() + "/activate")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.active").value(true));
		assertThat(reload(card).isActive()).isTrue();
	}

	@Test
	@DisplayName("Deactivating or activating a card that does not exist returns 404")
	void deactivateAndActivate_unknownId_return404() throws Exception {
		// Act + Assert
		mockMvc.perform(asAdmin(post(BASE_PATH + "/" + Integer.MAX_VALUE + "/deactivate")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"));
		mockMvc.perform(asAdmin(post(BASE_PATH + "/" + Integer.MAX_VALUE + "/activate")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"));
	}

	// --- documentazione OpenAPI ---

	@Test
	@DisplayName("OpenAPI requires the Bearer token on every admin card endpoint")
	void apiDocs_adminCards_requireBearerToken() throws Exception {
		// Act + Assert: @SecurityRequirement è sulla classe, quindi vale per tutti i metodi
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/admin/cards'].get.security[0]").value(hasKey("bearerAuth")))
			.andExpect(jsonPath("$.paths['/api/v1/admin/cards/{id}'].put.security[0]").value(hasKey("bearerAuth")))
			.andExpect(jsonPath("$.paths['/api/v1/admin/cards/{id}/deactivate'].post.security[0]").value(hasKey("bearerAuth")))
			.andExpect(jsonPath("$.paths['/api/v1/admin/cards/{id}/activate'].post.security[0]").value(hasKey("bearerAuth")));
	}

	@Test
	@DisplayName("OpenAPI has no create endpoint yet and no image path in the update form")
	void apiDocs_adminCards_noCreateAndNoImagePath() throws Exception {
		// Act + Assert: POST /admin/cards e l'immagine arrivano con API-14
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/admin/cards'].post").doesNotExist())
			.andExpect(jsonPath("$.components.schemas.CatalogCardUpdateRequest.properties.name").exists())
			.andExpect(jsonPath("$.components.schemas.CatalogCardUpdateRequest.properties.imagePath").doesNotExist());
	}

	// --- supporto ---

	private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.createAdmin()));
	}

	private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.create()));
	}

	private ResultActions putCard(Integer id, String json) throws Exception {
		return mockMvc.perform(asAdmin(put(BASE_PATH + "/" + id)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private CatalogCard reload(CatalogCard card) {
		return catalogCardRepository.findById(card.getId()).orElseThrow();
	}

	// Creatura valida (ATK 7, DEF 8); variantOfId null = originale
	private static String creatureBody(String name, Integer variantOfId) {
		return body(name, CardType.CREATURE, null, null, variantOfId, 7, 8, null);
	}

	// I null diventano il JSON null, come li manderebbe il frontend per un campo vuoto
	private static String body(String name, CardType type, CardSubtype subtype, String effectCode, Integer variantOfId,
			Integer atk, Integer def, Integer copyLimit) {
		return """
			{"name": %s, "type": %s, "subtype": %s, "effectCode": %s, "variantOfId": %s, "atk": %s, "def": %s, "copyLimit": %s,
			"effectDescription": "Effect text", "flavorText": "Flavor text"}
			""".formatted(quoted(name), quoted(type), quoted(subtype), quoted(effectCode), variantOfId, atk, def, copyLimit);
	}

	private static String quoted(Object value) {
		return value == null ? "null" : "\"" + value + "\"";
	}
}
