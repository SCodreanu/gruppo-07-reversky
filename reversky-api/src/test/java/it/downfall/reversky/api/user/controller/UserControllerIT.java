package it.downfall.reversky.api.user.controller;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * {@link UserController} via HTTP, con i filtri di Spring Security attivi.
 * <p>
 * L'access token si crea direttamente con {@link JwtService}: il login è già coperto da AuthControllerIT.
 * Il 401 senza token è in AuthControllerIT, insieme alle altre regole generali di SecurityConfig.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	// Non serve al test: con la stessa configurazione di AuthControllerIT, Spring riusa
	// il contesto già avviato (e il suo container) invece di crearne uno nuovo.
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("A valid token returns the profile of its user")
	void me_validToken_returnsProfile() throws Exception {
		// Arrange
		User user = testUsers.create();

		// Act + Assert: il ruolo è LAZY, quindi questo test verifica anche che venga letto dentro la transazione
		me(jwtService.createAccessToken(user))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(user.getId()))
			.andExpect(jsonPath("$.username").value(user.getUsername()))
			.andExpect(jsonPath("$.email").value(user.getEmail()))
			.andExpect(jsonPath("$.role").value("USER"))
			.andExpect(jsonPath("$.emailVerified").value(false));
	}

	@Test
	@DisplayName("emailVerified is true once the email has been verified")
	void me_verifiedUser_returnsEmailVerifiedTrue() throws Exception {
		// Arrange
		User user = testUsers.create();
		testUsers.markEmailVerified(user);

		// Act + Assert
		me(jwtService.createAccessToken(user))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.emailVerified").value(true));
	}

	@Test
	@DisplayName("A still valid token of a deleted user returns 404")
	void me_deletedUser_returns404() throws Exception {
		// Arrange: il token resta valido fino alla scadenza anche se l'utente non esiste più
		User user = testUsers.create();
		String accessToken = jwtService.createAccessToken(user);
		testUsers.delete(user);

		// Act + Assert
		me(accessToken)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	@DisplayName("OpenAPI requires the Bearer token on /users/me")
	void apiDocs_me_requiresBearerToken() throws Exception {
		// Act + Assert: @SecurityRequirement è sulla classe, quindi vale per ogni endpoint del controller
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/users/me'].get.security[0]").value(hasKey("bearerAuth")));
	}

	private ResultActions me(String accessToken) throws Exception {
		return mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
	}

}
