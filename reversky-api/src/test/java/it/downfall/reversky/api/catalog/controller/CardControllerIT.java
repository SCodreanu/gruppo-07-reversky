package it.downfall.reversky.api.catalog.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.entity.CardSubtype;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link CardController} via HTTP: il catalogo pubblico, senza token.
 * <p>
 * Le regole dei filtri e della paginazione sono già in CatalogCardServiceIT; qui si verificano
 * l'accesso pubblico, i parametri della query, il JSON e la documentazione OpenAPI.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class CardControllerIT {

	private static final String BASE_PATH = "/api/v1/cards";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CatalogCardRepository catalogCardRepository;

	@Autowired
	private RarityRepository rarityRepository;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa
	// il contesto già avviato (e il suo container) invece di crearne uno nuovo.
	@MockitoBean
	private EmailSender emailSender;

	private TestCards testCards;

	@BeforeEach
	void setUp() {
		testCards = new TestCards(catalogCardRepository, rarityRepository);
	}

	// --- GET /cards ---

	@Test
	@DisplayName("The card list is public and returns the agreed page format")
	void findAll_withoutToken_returnsPage() throws Exception {
		// Arrange: il token nel nome limita la ricerca alle carte di questo test
		String token = TestUsers.uniqueSuffix();
		CatalogCard card = testCards.create("Spell " + token, CardType.SPELL);

		// Act + Assert
		mockMvc.perform(get(BASE_PATH).param("name", token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(card.getId()))
			.andExpect(jsonPath("$.content[0].name").value(card.getName()))
			.andExpect(jsonPath("$.content[0].type").value("SPELL"))
			.andExpect(jsonPath("$.content[0].subtype").value("BURN"))
			.andExpect(jsonPath("$.content[0].effectCode").value(TestCards.EFFECT_CODE))
			.andExpect(jsonPath("$.content[0].rarity.id").value(testCards.common().getId()))
			.andExpect(jsonPath("$.content[0].rarity.name").value(testCards.common().getName()))
			.andExpect(jsonPath("$.content[0].active").value(true))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(20))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.totalPages").value(1));
	}

	@Test
	@DisplayName("The public list does not show deactivated cards")
	void findAll_deactivatedCard_isNotListed() throws Exception {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		CatalogCard inactive = testCards.deactivate(testCards.create("Inactive " + token, CardType.CREATURE));

		// Act + Assert
		mockMvc.perform(get(BASE_PATH).param("name", token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id", not(hasItem(inactive.getId()))))
			.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	@DisplayName("The type parameter filters the list")
	void findAll_typeParameter_filtersByType() throws Exception {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		CatalogCard terrain = testCards.create("Terrain " + token, CardType.TERRAIN);
		testCards.create("Creature " + token, CardType.CREATURE);

		// Act + Assert
		mockMvc.perform(get(BASE_PATH).param("name", token).param("type", "TERRAIN"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(terrain.getId()));
	}

	@Test
	@DisplayName("The rarityId parameter filters the list")
	void findAll_rarityIdParameter_filtersByRarity() throws Exception {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		Rarity other = testCards.createRarity(null, null);
		CatalogCard otherCard = testCards.create("Other " + token, CardType.CREATURE, other);
		testCards.create("Common " + token, CardType.CREATURE);

		// Act + Assert
		mockMvc.perform(get(BASE_PATH).param("name", token).param("rarityId", other.getId().toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(otherCard.getId()))
			.andExpect(jsonPath("$.content[0].rarity.id").value(other.getId()));
	}

	@Test
	@DisplayName("An unknown rarityId returns an empty page, not 404")
	void findAll_unknownRarityId_returnsEmptyPage() throws Exception {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		testCards.create("Common " + token, CardType.CREATURE);

		// Act + Assert
		mockMvc.perform(get(BASE_PATH).param("name", token).param("rarityId", String.valueOf(Integer.MAX_VALUE)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	@DisplayName("A rarityId that is not a number returns 400")
	void findAll_rarityIdNotNumber_returns400() throws Exception {
		// Act + Assert
		mockMvc.perform(get(BASE_PATH).param("rarityId", "rare"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("An unknown type returns 400")
	void findAll_unknownType_returns400() throws Exception {
		// Act + Assert
		mockMvc.perform(get(BASE_PATH).param("type", "DRAGON"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("A sort parameter with an unknown property is ignored, not a 500")
	void findAll_unknownSortProperty_returns200() throws Exception {
		// Act + Assert
		mockMvc.perform(get(BASE_PATH).param("sort", "doesNotExist,desc"))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("A page size above 100 is reduced to 100")
	void findAll_sizeAboveMaximum_returns100() throws Exception {
		// Act + Assert
		mockMvc.perform(get(BASE_PATH).param("size", "500"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.size").value(100));
	}

	// --- GET /cards/{id} ---

	@Test
	@DisplayName("A card is public and returns every field")
	void findById_withoutToken_returnsCard() throws Exception {
		// Arrange
		CatalogCard card = testCards.createCreature();

		// Act + Assert: i campi vuoti arrivano come null, non spariscono dal JSON
		mockMvc.perform(get(BASE_PATH + "/" + card.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(card.getId()))
			.andExpect(jsonPath("$.name").value(card.getName()))
			.andExpect(jsonPath("$.type").value("CREATURE"))
			.andExpect(jsonPath("$.subtype").isEmpty())
			.andExpect(jsonPath("$.effectCode").isEmpty())
			.andExpect(jsonPath("$.variantOfId").isEmpty())
			.andExpect(jsonPath("$.rarity.id").value(card.getRarity().getId()))
			.andExpect(jsonPath("$.rarity.name").value(testCards.common().getName()))
			.andExpect(jsonPath("$.atk").value(TestCards.CREATURE_ATK))
			.andExpect(jsonPath("$.def").value(TestCards.CREATURE_DEF))
			.andExpect(jsonPath("$.copyLimit").isEmpty())
			.andExpect(jsonPath("$.imagePath").value(TestCards.IMAGE_PATH))
			.andExpect(jsonPath("$.effectDescription").value("Test effect"))
			.andExpect(jsonPath("$.flavorText").value("Test flavor"))
			.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	@DisplayName("A deactivated card can still be read by id")
	void findById_deactivatedCard_returns200() throws Exception {
		// Arrange
		CatalogCard card = testCards.deactivate(testCards.createCreature());

		// Act + Assert
		mockMvc.perform(get(BASE_PATH + "/" + card.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	@DisplayName("A variant shows the id of its original")
	void findById_variant_returnsVariantOfId() throws Exception {
		// Arrange
		CatalogCard original = testCards.create(TestCards.uniqueName("Hidden"), CardType.HIDDEN);
		CatalogCard variant = testCards.createVariantOf(original);

		// Act + Assert
		mockMvc.perform(get(BASE_PATH + "/" + variant.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.variantOfId").value(original.getId()))
			.andExpect(jsonPath("$.rarity.id").value(original.getRarity().getId()))
			.andExpect(jsonPath("$.subtype").value(CardSubtype.SURVIVAL.name()));
	}

	@Test
	@DisplayName("A card that does not exist returns 404")
	void findById_unknownId_returns404() throws Exception {
		// Act + Assert
		mockMvc.perform(get(BASE_PATH + "/" + Integer.MAX_VALUE))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"));
	}

	// --- documentazione OpenAPI ---

	@Test
	@DisplayName("OpenAPI does not ask for a token on the public card endpoints")
	void apiDocs_cards_haveNoSecurity() throws Exception {
		// Act + Assert
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/cards'].get").exists())
			.andExpect(jsonPath("$.paths['/api/v1/cards'].get.security").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/cards/{id}'].get.security").doesNotExist());
	}

	@Test
	@DisplayName("OpenAPI documents type, name, rarityId, page and size, but not sort")
	void apiDocs_cardList_documentsOnlyUsedParameters() throws Exception {
		// Act + Assert: il sort del client si ignora, quindi Swagger non lo deve offrire
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/cards'].get.parameters[*].name", hasItems("type", "name", "rarityId", "page", "size")))
			.andExpect(jsonPath("$.paths['/api/v1/cards'].get.parameters[*].name", not(hasItem("sort"))))
			.andExpect(jsonPath("$.paths['/api/v1/cards'].get.parameters[*].name", not(hasItem("pageable"))));
	}
}
