package it.downfall.reversky.api.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.Role;
import it.downfall.reversky.api.user.entity.User;

/**
 * Un access token scaduto o non valido sugli endpoint pubblici (API-58, docs/autenticazione): si ignora, come se non
 * ci fosse. Il frontend può mandare sempre l'header {@code Authorization}, anche al rinnovo, quando l'access token è
 * già scaduto. Sugli endpoint protetti lo stesso token resta un {@code 401}.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class PublicEndpointTokenIT {

	private static final String INVALID = "Bearer not-a-jwt";
	// Uno spazio nel token: DefaultBearerTokenResolver lo rifiuta prima di decodificarlo
	private static final String MALFORMED = "Bearer two words";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("Refresh with an expired access token in the header still reaches the refresh check")
	void refresh_expiredAccessToken_ignored() throws Exception {
		// Act + Assert: il refresh token è falso, quindi la risposta giusta è il 401 del rinnovo, non AUTH_REQUIRED
		mockMvc.perform(post("/api/v1/auth/refresh").header(HttpHeaders.AUTHORIZATION, expiredToken())
				.contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\": \"unknown\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REFRESH_TOKEN_INVALID"));
	}

	@Test
	@DisplayName("Login with an expired access token in the header returns 200 with new tokens")
	void login_expiredAccessToken_ignored() throws Exception {
		// Arrange
		User user = testUsers.create();

		// Act + Assert
		mockMvc.perform(post("/api/v1/auth/login").header(HttpHeaders.AUTHORIZATION, expiredToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + user.getEmail() + "\", \"password\": \"" + TestUsers.PASSWORD + "\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}

	@Test
	@DisplayName("Public GETs answer 200 with an expired, invalid or malformed token, as without a token")
	void publicGets_staleToken_ignored() throws Exception {
		for (String path : new String[] {"/api/v1/cards", "/api/v1/factions", "/api/v1/rarities", "/api/v1/rules"}) {
			for (String header : new String[] {expiredToken(), INVALID, MALFORMED}) {
				mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, header))
					.andExpect(status().isOk());
			}
		}
	}

	@Test
	@DisplayName("The shop answers 200 with an expired token (anonymous shop) and with a valid one")
	void packs_expiredOrValidToken_returns200() throws Exception {
		// Arrange: che con un token valido si legga remainingPurchases lo controlla PackPurchaseControllerIT
		User user = testUsers.create();

		// Act + Assert
		mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, expiredToken()))
			.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION,
				"Bearer " + jwtService.createAccessToken(user)))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Protected endpoints still reject an expired token with 401, also the email resend under /auth")
	void protectedEndpoints_expiredToken_401() throws Exception {
		mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, expiredToken()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
		mockMvc.perform(post("/api/v1/auth/email/resend").header(HttpHeaders.AUTHORIZATION, expiredToken()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	// Firmato con la chiave vera, come uno emesso un'ora fa e scaduto da 45 minuti
	private String expiredToken() {
		Instant issued = Instant.now().minus(Duration.ofHours(1));
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject("1")
			.issuedAt(issued)
			.expiresAt(issued.plus(Duration.ofMinutes(15)))
			.claim(JwtService.CLAIM_USERNAME, "expired")
			.claim(JwtService.CLAIM_ROLE, Role.USER)
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
