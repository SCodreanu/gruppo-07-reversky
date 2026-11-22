package it.downfall.reversky.api.common.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestUsers;

/**
 * Swagger UI e documento OpenAPI raggiungibili senza token (docs/api-rest, "Swagger / OpenAPI").
 * <p>
 * Lo schema {@code bearerAuth} e il suo uso sugli endpoint protetti sono controllati in
 * AuthControllerIT e UserControllerIT, vicino agli endpoint a cui si riferiscono.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiConfigIT {

	@Autowired
	private MockMvc mockMvc;

	// Non serve al test: con la stessa configurazione di AuthControllerIT, Spring riusa
	// il contesto già avviato (e il suo container) invece di crearne uno nuovo.
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("/swagger-ui.html redirects to the Swagger UI page")
	void swaggerUiPath_withoutToken_redirectsToIndex() throws Exception {
		// Act + Assert: è l'indirizzo di springdoc.swagger-ui.path in application.properties
		mockMvc.perform(get("/swagger-ui.html"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string(HttpHeaders.LOCATION, containsString("/swagger-ui/index.html")));
	}

	@Test
	@DisplayName("The Swagger UI page is served without a token")
	void swaggerUiIndex_withoutToken_returnsHtml() throws Exception {
		// Act + Assert
		mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
	}

	@Test
	@DisplayName("The OpenAPI document has the title and version of OpenApiConfig")
	void apiDocs_withoutToken_returnsApiInfo() throws Exception {
		// Act + Assert
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.info.title").value("Reversky API"))
			.andExpect(jsonPath("$.info.version").value("v1"));
	}
}
