package it.downfall.reversky.api.admin.controller;

import static org.hamcrest.Matchers.hasSize;
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

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * {@link AdminEffectController} via HTTP (API-36): accesso ed elenco degli effetti di V13.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class AdminEffectControllerIT {

	private static final String BASE_PATH = "/api/v1/admin/effects";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	// Non serve al test: stessa configurazione degli altri ControllerIT, così Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("Without a token the effect list returns 401")
	void findAll_withoutToken_returns401() throws Exception {
		mockMvc.perform(get(BASE_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("A USER cannot read the effect list")
	void findAll_userRole_returns403() throws Exception {
		mockMvc.perform(get(BASE_PATH).header(HttpHeaders.AUTHORIZATION, bearer(testUsers.create())))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	@DisplayName("An ADMIN gets the 92 effects, sorted by card type and then by code")
	void findAll_admin_returnsSortedEffects() throws Exception {
		// Ordine dell'enum card_type di V1: CREATURE, TERRAIN, SPELL, HIDDEN, ITEM
		mockMvc.perform(get(BASE_PATH).header(HttpHeaders.AUTHORIZATION, bearer(testUsers.createAdmin())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(92)))
			.andExpect(jsonPath("$[0].code").value("BAKU"))
			.andExpect(jsonPath("$[0].appliesTo").value("CREATURE"))
			.andExpect(jsonPath("$[64].code").value("DEVOURING_ABYSS"))
			.andExpect(jsonPath("$[64].appliesTo").value("TERRAIN"))
			.andExpect(jsonPath("$[68].code").value("BITE_OF_REVERSE"))
			.andExpect(jsonPath("$[68].appliesTo").value("SPELL"))
			.andExpect(jsonPath("$[91].code").value("TORII_BELL"))
			.andExpect(jsonPath("$[91].appliesTo").value("ITEM"));
	}

	private String bearer(User user) {
		return "Bearer " + jwtService.createAccessToken(user);
	}
}
