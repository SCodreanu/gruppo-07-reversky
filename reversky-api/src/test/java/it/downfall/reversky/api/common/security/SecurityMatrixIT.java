package it.downfall.reversky.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestUsers;

/**
 * Matrice degli accessi di tutti gli endpoint (API-58, docs/autenticazione, "Endpoint pubblici e protetti").
 * <p>
 * Ogni riga dice chi può chiamare l'endpoint: {@code PUBLIC} senza token, {@code USER} con un token qualsiasi,
 * {@code ADMIN} solo con il ruolo ADMIN. Per ogni riga si controlla che senza token un endpoint protetto dia
 * {@code 401}, che un {@code USER} su {@code /admin/**} riceva {@code 403} e che chi ha diritto non riceva né
 * {@code 401} né {@code 403}. Le richieste usano id che non esistono e un corpo vuoto, quindi si fermano a
 * {@code 400} o {@code 404} e non cambiano niente nel database.
 * <p>
 * {@link #matrix_coversEveryEndpoint()} legge gli endpoint da Spring: un endpoint nuovo senza la sua riga fa
 * fallire il test. I casi "risorsa di un altro utente → 404" sono nei test dei singoli endpoint.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityMatrixIT {

	private static final String API = "/api/v1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	private RequestMappingHandlerMapping handlerMapping;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	private String userToken;
	private String adminToken;

	@BeforeAll
	void createUsers() {
		userToken = "Bearer " + jwtService.createAccessToken(testUsers.create());
		adminToken = "Bearer " + jwtService.createAccessToken(testUsers.createAdmin());
	}

	/** Chi può chiamare un endpoint. */
	enum Access {
		PUBLIC, USER, ADMIN
	}

	/**
	 * Una riga della matrice.
	 *
	 * @param handler {@code false} per i percorsi che non sono di un controller (le immagini)
	 */
	record Endpoint(HttpMethod method, String path, Access access, boolean handler) {

		Endpoint(HttpMethod method, String path, Access access) {
			this(method, path, access, true);
		}

		@Override
		public String toString() {
			return method + " " + path + " → " + access;
		}
	}

	static Stream<Endpoint> endpoints() {
		return Stream.of(
			// Autenticazione: pubblica, tranne il reinvio della verifica
			pub(HttpMethod.POST, "/auth/register"), pub(HttpMethod.POST, "/auth/login"),
			pub(HttpMethod.POST, "/auth/refresh"), pub(HttpMethod.POST, "/auth/logout"),
			pub(HttpMethod.POST, "/auth/password/forgot"), pub(HttpMethod.POST, "/auth/password/reset"),
			pub(HttpMethod.POST, "/auth/email/verify"), user(HttpMethod.POST, "/auth/email/resend"),
			// Catalogo e regole pubblici
			pub(HttpMethod.GET, "/cards"), pub(HttpMethod.GET, "/cards/{id}"), pub(HttpMethod.GET, "/factions"),
			pub(HttpMethod.GET, "/rarities"), pub(HttpMethod.GET, "/rules"), pub(HttpMethod.GET, "/packs"),
			new Endpoint(HttpMethod.GET, "/images/{file}", Access.PUBLIC, false),
			// Utente
			user(HttpMethod.GET, "/users/me"), user(HttpMethod.GET, "/inventory"),
			user(HttpMethod.POST, "/packs/{id}/purchase"), user(HttpMethod.POST, "/inventory/packs/{packId}/open"),
			user(HttpMethod.GET, "/collection"), user(HttpMethod.GET, "/collection/summary"),
			user(HttpMethod.GET, "/users/{id}/collection"),
			user(HttpMethod.GET, "/decks"), user(HttpMethod.POST, "/decks"), user(HttpMethod.GET, "/decks/rules"),
			user(HttpMethod.POST, "/decks/validate"), user(HttpMethod.GET, "/decks/{id}"),
			user(HttpMethod.PUT, "/decks/{id}"), user(HttpMethod.DELETE, "/decks/{id}"),
			user(HttpMethod.GET, "/friends"), user(HttpMethod.GET, "/friends/requests"),
			user(HttpMethod.POST, "/friends/requests"), user(HttpMethod.POST, "/friends/requests/{requesterId}/accept"),
			user(HttpMethod.POST, "/friends/requests/{requesterId}/reject"), user(HttpMethod.DELETE, "/friends/{userId}"),
			user(HttpMethod.GET, "/trades"), user(HttpMethod.POST, "/trades"), user(HttpMethod.GET, "/trades/{id}"),
			user(HttpMethod.POST, "/trades/{id}/counter"), user(HttpMethod.POST, "/trades/{id}/accept"),
			user(HttpMethod.POST, "/trades/{id}/reject"),
			user(HttpMethod.GET, "/battle-invites"), user(HttpMethod.POST, "/battle-invites"),
			user(HttpMethod.POST, "/battle-invites/{id}/accept"), user(HttpMethod.POST, "/battle-invites/{id}/reject"),
			user(HttpMethod.DELETE, "/battle-invites/{id}"),
			user(HttpMethod.GET, "/battles"), user(HttpMethod.GET, "/battles/{id}"),
			user(HttpMethod.POST, "/battles/{id}/retry"), user(HttpMethod.GET, "/rankings/me"),
			user(HttpMethod.POST, "/matchmaking/queue"), user(HttpMethod.GET, "/matchmaking/queue"),
			user(HttpMethod.DELETE, "/matchmaking/queue"),
			// Backoffice
			admin(HttpMethod.GET, "/admin/users"), admin(HttpMethod.POST, "/admin/users/{id}/disable"),
			admin(HttpMethod.POST, "/admin/users/{id}/enable"), admin(HttpMethod.PUT, "/admin/users/{id}/role"),
			admin(HttpMethod.POST, "/admin/users/{id}/credits"),
			admin(HttpMethod.GET, "/admin/cards"), admin(HttpMethod.POST, "/admin/cards"),
			admin(HttpMethod.PUT, "/admin/cards/{id}"), admin(HttpMethod.POST, "/admin/cards/{id}/image"),
			admin(HttpMethod.POST, "/admin/cards/{id}/deactivate"), admin(HttpMethod.POST, "/admin/cards/{id}/activate"),
			admin(HttpMethod.GET, "/admin/effects"),
			admin(HttpMethod.GET, "/admin/faction-bonuses"), admin(HttpMethod.PUT, "/admin/faction-bonuses"),
			admin(HttpMethod.GET, "/admin/factions"), admin(HttpMethod.POST, "/admin/factions"),
			admin(HttpMethod.PUT, "/admin/factions/{id}"),
			admin(HttpMethod.GET, "/admin/rarities"), admin(HttpMethod.POST, "/admin/rarities"),
			admin(HttpMethod.PUT, "/admin/rarities/{id}"),
			admin(HttpMethod.GET, "/admin/packs"), admin(HttpMethod.POST, "/admin/packs"),
			admin(HttpMethod.PUT, "/admin/packs/{id}"), admin(HttpMethod.POST, "/admin/packs/{id}/activate"),
			admin(HttpMethod.POST, "/admin/packs/{id}/deactivate"),
			admin(HttpMethod.GET, "/admin/rule-sets"), admin(HttpMethod.POST, "/admin/rule-sets"),
			admin(HttpMethod.PUT, "/admin/rule-sets/{id}"), admin(HttpMethod.POST, "/admin/rule-sets/{id}/activate"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("endpoints")
	@DisplayName("Without a token: protected endpoints return 401 AUTH_REQUIRED, public ones neither 401 nor 403")
	void withoutToken(Endpoint endpoint) throws Exception {
		if (endpoint.access() == Access.PUBLIC) {
			assertAllowed(endpoint, null);
		} else {
			mockMvc.perform(call(endpoint))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("endpoints")
	@DisplayName("With a USER token: /admin/** returns 403 ACCESS_DENIED, everything else neither 401 nor 403")
	void withUserToken(Endpoint endpoint) throws Exception {
		if (endpoint.access() == Access.ADMIN) {
			mockMvc.perform(call(endpoint).header(HttpHeaders.AUTHORIZATION, userToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		} else {
			assertAllowed(endpoint, userToken);
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("endpoints")
	@DisplayName("With an ADMIN token every endpoint is reachable: neither 401 nor 403")
	void withAdminToken(Endpoint endpoint) throws Exception {
		assertAllowed(endpoint, adminToken);
	}

	@Test
	@DisplayName("The matrix has exactly one row for every endpoint of the controllers")
	void matrix_coversEveryEndpoint() {
		// Arrange: gli endpoint registrati in Spring sotto /api/v1, come "METODO percorso"
		Set<String> mapped = new TreeSet<>();
		handlerMapping.getHandlerMethods().keySet().forEach(info -> info.getPatternValues().stream()
			.filter(pattern -> pattern.startsWith(API))
			.forEach(pattern -> info.getMethodsCondition().getMethods()
				.forEach(method -> mapped.add(method.name() + " " + pattern))));

		// Act
		List<String> rows = endpoints().filter(Endpoint::handler)
			.map(endpoint -> endpoint.method().name() + " " + API + endpoint.path())
			.toList();

		// Assert: nessuna riga doppia, nessuna mancante e nessuna in più
		assertThat(rows).doesNotHaveDuplicates();
		assertThat(new TreeSet<>(rows)).isEqualTo(mapped);
	}

	// Né 401 né 403: la richiesta passa la sicurezza e si ferma nel controller (di solito 400 o 404)
	private void assertAllowed(Endpoint endpoint, String token) throws Exception {
		MockHttpServletRequestBuilder builder = call(endpoint);
		if (token != null) {
			builder.header(HttpHeaders.AUTHORIZATION, token);
		}
		int status = mockMvc.perform(builder).andReturn().getResponse().getStatus();
		assertThat(status).as(endpoint + " with " + (token == null ? "no token" : "a token")).isNotIn(401, 403);
	}

	// Id che non esistono (UUID dove il percorso li vuole) e, per chi ha un corpo, un JSON vuoto: 400 prima del service
	private static MockHttpServletRequestBuilder call(Endpoint endpoint) {
		String root = endpoint.handler() ? API : "";
		String path = endpoint.path().replaceAll("\\{[^}]+}", isUuidPath(endpoint.path()) ? UUID.randomUUID().toString()
			: endpoint.handler() ? "999999999" : "missing.png");
		MockHttpServletRequestBuilder builder = request(endpoint.method(), root + path);
		if (endpoint.method() != HttpMethod.GET && endpoint.method() != HttpMethod.DELETE) {
			builder.contentType(MediaType.APPLICATION_JSON).content("");
		}
		return builder;
	}

	private static boolean isUuidPath(String path) {
		return path.startsWith("/trades/") || path.startsWith("/battles/") || path.startsWith("/battle-invites/");
	}

	private static Endpoint pub(HttpMethod method, String path) {
		return new Endpoint(method, path, Access.PUBLIC);
	}

	private static Endpoint user(HttpMethod method, String path) {
		return new Endpoint(method, path, Access.USER);
	}

	private static Endpoint admin(HttpMethod method, String path) {
		return new Endpoint(method, path, Access.ADMIN);
	}
}
