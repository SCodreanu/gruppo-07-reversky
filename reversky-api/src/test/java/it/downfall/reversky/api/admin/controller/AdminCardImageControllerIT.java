package it.downfall.reversky.api.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.common.storage.ImageStorage;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestUsers;

/**
 * Creazione delle carte con l'illustrazione e sostituzione dell'illustrazione (API-14, #84), via HTTP con il database
 * vero e la cartella delle immagini dei test ({@code build/test-images}, da build.gradle).
 * <p>
 * Le regole dei campi della carta sono le stesse del PUT, già in {@code AdminCardControllerIT} e
 * {@code CatalogCardServiceIT}: qui se ne prova una sola, per vedere che la carta non si crea e il file non si scrive.
 * Il limite di Spring sul multipart ({@code 413}) non si prova: MockMvc non passa dal parser del servlet.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class AdminCardImageControllerIT {

	private static final String BASE_PATH = "/api/v1/admin/cards";

	// Solo i primi byte contano: il tipo si riconosce dalla firma del file
	private static final byte[] PNG = withSignature(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 64);
	private static final byte[] JPEG = withSignature(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, 64);
	private static final byte[] WEBP = withSignature(new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}, 64);

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
	private ImageStorage imageStorage;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	private TestCards testCards;

	@BeforeEach
	void setUp() {
		testCards = new TestCards(catalogCardRepository, rarityRepository, factionRepository);
	}

	// --- POST /admin/cards ---

	@Test
	@DisplayName("A card is created with its image: 201, a new active card, and the image is public at imageUrl")
	void create_withImage_createsCardAndServesImage() throws Exception {
		// Arrange
		String name = TestCards.uniqueName("Created");

		// Act
		String body = create(creatureJson(name, null), image("art.png", PNG))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value(name))
			.andExpect(jsonPath("$.type").value("CREATURE"))
			.andExpect(jsonPath("$.atk").value(7))
			.andExpect(jsonPath("$.active").value(true))
			.andReturn().getResponse().getContentAsString();

		// Assert: percorso nuovo in cards/, URL pubblico senza token con gli stessi byte
		String imagePath = JsonPath.read(body, "$.imagePath");
		String imageUrl = JsonPath.read(body, "$.imageUrl");
		assertThat(imagePath).startsWith("cards/").endsWith(".png");
		assertThat(imageUrl).isEqualTo("/images/" + imagePath);
		mockMvc.perform(get(imageUrl))
			.andExpect(status().isOk())
			.andExpect(content().bytes(PNG))
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE));
		Integer id = JsonPath.read(body, "$.id");
		assertThat(catalogCardRepository.findById(id)).get()
			.satisfies(card -> assertThat(card.getImagePath()).isEqualTo(imagePath));
		mockMvc.perform(get("/api/v1/cards/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imageUrl").value(imageUrl));
	}

	@Test
	@DisplayName("JPEG and WebP are accepted too, recognized from their content")
	void create_jpegAndWebp_areAccepted() throws Exception {
		// Act + Assert: il nome del file e il Content-Type del client non contano
		create(creatureJson(TestCards.uniqueName("Jpeg"), null), image("art.bin", JPEG))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.imagePath").value(org.hamcrest.Matchers.endsWith(".jpg")));
		create(creatureJson(TestCards.uniqueName("Webp"), null), image("art.png", WEBP))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.imagePath").value(org.hamcrest.Matchers.endsWith(".webp")));
	}

	@Test
	@DisplayName("A variant is created with the game fields of its original, its own name and image")
	void create_variant_copiesGameFieldsFromOriginal() throws Exception {
		// Arrange: la richiesta dice ATK 7 e DEF 8, ma per una variante valgono quelli dell'originale
		CatalogCard original = testCards.createCreature();

		// Act + Assert
		create(creatureJson(original.getName(), original.getId()), image("variant.png", PNG))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.variantOfId").value(original.getId()))
			.andExpect(jsonPath("$.name").value(original.getName()))
			.andExpect(jsonPath("$.atk").value(original.getAtk()))
			.andExpect(jsonPath("$.def").value(original.getDef()))
			.andExpect(jsonPath("$.rarity.id").value(original.getRarity().getId()))
			.andExpect(jsonPath("$.imagePath").value(org.hamcrest.Matchers.startsWith("cards/")));
	}

	@Test
	@DisplayName("Without the image part the card is not created: 400")
	void create_withoutImage_returns400AndCreatesNothing() throws Exception {
		// Arrange
		String name = TestCards.uniqueName("NoImage");

		// Act + Assert
		mockMvc.perform(asAdmin(multipart(BASE_PATH).file(cardPart(creatureJson(name, null)))))
			.andExpect(status().isBadRequest());
		assertThat(cardExists(name)).isFalse();
	}

	@Test
	@DisplayName("A file that is not PNG, JPEG or WebP, an empty one or one over 2 MB returns 422 and creates nothing")
	void create_invalidImage_returns422AndLeavesNoFile() throws Exception {
		// Arrange
		long filesBefore = storedFiles();
		byte[] tooLarge = withSignature(PNG, 2 * 1024 * 1024 + 1);

		// Act + Assert
		assertInvalidImage(image("notes.png", "just some text".getBytes(StandardCharsets.UTF_8)),
			"The image must be a PNG, JPEG or WebP file");
		assertInvalidImage(image("empty.png", new byte[0]), "The image is empty");
		assertInvalidImage(image("huge.png", tooLarge), "The image must not exceed 2 MB");
		assertThat(storedFiles()).isEqualTo(filesBefore);
	}

	@Test
	@DisplayName("A card that breaks the card rules returns 422 and no image file is written")
	void create_invalidCard_returns422AndWritesNoFile() throws Exception {
		// Arrange: una Spell non può avere ATK e DEF (regole del PUT)
		long filesBefore = storedFiles();
		String name = TestCards.uniqueName("BadSpell");
		String spell = """
			{"name": "%s", "type": "SPELL", "rarityId": %d, "subtype": "DAMAGE", "effectCode": "%s", "atk": 3, "def": 3}
			""".formatted(name, testCards.rare().getId(), TestCards.effectCodeFor(CardType.SPELL));

		// Act + Assert
		create(spell, image("spell.png", PNG))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("CARD_RULES_VIOLATED"));
		assertThat(cardExists(name)).isFalse();
		assertThat(storedFiles()).isEqualTo(filesBefore);
	}

	@Test
	@DisplayName("A new original card with the name of another original returns 409 CARD_NAME_TAKEN")
	void create_nameOfAnotherOriginal_returns409() throws Exception {
		// Arrange
		CatalogCard existing = testCards.createCreature();

		// Act + Assert
		create(creatureJson(existing.getName(), null), image("dup.png", PNG))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CARD_NAME_TAKEN"));
		assertThat(catalogCardRepository.findAll().stream().filter(card -> card.getName().equals(existing.getName())))
			.hasSize(1);
	}

	@Test
	@DisplayName("Without a token creating a card returns 401, as a USER 403")
	void create_withoutAdmin_isRejected() throws Exception {
		// Act + Assert
		mockMvc.perform(multipart(BASE_PATH).file(cardPart(creatureJson(TestCards.uniqueName("Anon"), null)))
				.file(image("a.png", PNG)))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(asUser(multipart(BASE_PATH).file(cardPart(creatureJson(TestCards.uniqueName("User"), null)))
				.file(image("a.png", PNG))))
			.andExpect(status().isForbidden());
	}

	// --- POST /admin/cards/{id}/image ---

	@Test
	@DisplayName("Replacing the image gives a new path and URL and deletes the old file")
	void replaceImage_valid_replacesAndDeletesOldFile() throws Exception {
		// Arrange
		String created = create(creatureJson(TestCards.uniqueName("Replace"), null), image("old.png", PNG))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		Integer id = JsonPath.read(created, "$.id");
		String oldPath = JsonPath.read(created, "$.imagePath");

		// Act
		String body = replace(id, image("new.webp", WEBP))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();

		// Assert
		String newPath = JsonPath.read(body, "$.imagePath");
		assertThat(newPath).isNotEqualTo(oldPath).endsWith(".webp");
		assertThat(catalogCardRepository.findById(id).orElseThrow().getImagePath()).isEqualTo(newPath);
		assertThat(Files.exists(imageStorage.getRoot().resolve(oldPath))).isFalse();
		mockMvc.perform(get("/images/" + newPath)).andExpect(status().isOk()).andExpect(content().bytes(WEBP));
		mockMvc.perform(get("/images/" + oldPath)).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("A card loaded by migration with a placeholder path gets a real image")
	void replaceImage_placeholder_isReplaced() throws Exception {
		// Arrange: la carta di prova ha un percorso segnaposto senza file, come quelle di V11
		CatalogCard card = testCards.createCreature();

		// Act + Assert
		replace(card.getId(), image("art.jpg", JPEG))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imagePath").value(org.hamcrest.Matchers.endsWith(".jpg")));
	}

	@Test
	@DisplayName("An invalid file returns 422 and the card keeps its image file and path")
	void replaceImage_invalid_keepsImage() throws Exception {
		// Arrange
		String created = create(creatureJson(TestCards.uniqueName("Keep"), null), image("keep.png", PNG))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		Integer id = JsonPath.read(created, "$.id");
		String path = JsonPath.read(created, "$.imagePath");
		long filesBefore = storedFiles();

		// Act + Assert
		replace(id, image("bad.png", "not an image".getBytes(StandardCharsets.UTF_8)))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("CARD_IMAGE_INVALID"));
		assertThat(catalogCardRepository.findById(id).orElseThrow().getImagePath()).isEqualTo(path);
		assertThat(Files.exists(imageStorage.getRoot().resolve(path))).isTrue();
		assertThat(storedFiles()).isEqualTo(filesBefore);
	}

	@Test
	@DisplayName("Replacing the image of an unknown card returns 404, of any card as a USER 403")
	void replaceImage_unknownOrUser_isRejected() throws Exception {
		// Arrange
		CatalogCard card = testCards.createCreature();

		// Act + Assert
		replace(Integer.MAX_VALUE, image("a.png", PNG))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"));
		mockMvc.perform(asUser(multipart(BASE_PATH + "/" + card.getId() + "/image").file(image("a.png", PNG))))
			.andExpect(status().isForbidden());
		assertThat(catalogCardRepository.findById(card.getId()).orElseThrow().getImagePath()).isEqualTo(card.getImagePath());
	}

	// --- helper ---

	private void assertInvalidImage(MockMultipartFile file, String detail) throws Exception {
		String name = TestCards.uniqueName("BadImage");
		create(creatureJson(name, null), file)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("CARD_IMAGE_INVALID"))
			.andExpect(jsonPath("$.detail").value(detail));
		assertThat(cardExists(name)).isFalse();
	}

	private ResultActions create(String cardJson, MockMultipartFile image) throws Exception {
		return mockMvc.perform(asAdmin(multipart(BASE_PATH).file(cardPart(cardJson)).file(image)));
	}

	private ResultActions replace(Integer id, MockMultipartFile image) throws Exception {
		return mockMvc.perform(asAdmin(multipart(BASE_PATH + "/" + id + "/image").file(image)));
	}

	// Creatura Common valida della fazione di prova; variantOfId null = originale
	private String creatureJson(String name, Integer variantOfId) {
		return """
			{"name": "%s", "type": "CREATURE", "rarityId": %d, "factionId": %d, "variantOfId": %s, "atk": 7, "def": 8}
			""".formatted(name, testCards.common().getId(), testCards.faction().getId(), variantOfId);
	}

	private static MockMultipartFile cardPart(String json) {
		return new MockMultipartFile("card", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
	}

	// Il Content-Type dichiarato è sempre image/png: il server deve guardare il contenuto
	private static MockMultipartFile image(String filename, byte[] content) {
		return new MockMultipartFile("image", filename, MediaType.IMAGE_PNG_VALUE, content);
	}

	private boolean cardExists(String name) {
		return catalogCardRepository.findAll().stream().anyMatch(card -> card.getName().equals(name));
	}

	// File nella cartella delle illustrazioni delle carte, per vedere che un errore non ne lascia
	private long storedFiles() throws IOException {
		Path cards = imageStorage.getRoot().resolve("cards");
		if (!Files.exists(cards)) {
			return 0;
		}
		try (Stream<Path> files = Files.list(cards)) {
			return files.count();
		}
	}

	private static byte[] withSignature(byte[] signature, int size) {
		byte[] content = Arrays.copyOf(signature, Math.max(size, signature.length));
		return content;
	}

	private MockMultipartHttpServletRequestBuilder asAdmin(MockMultipartHttpServletRequestBuilder request) {
		request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.createAdmin()));
		return request;
	}

	private MockMultipartHttpServletRequestBuilder asUser(MockMultipartHttpServletRequestBuilder request) {
		request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.create()));
		return request;
	}
}
