package it.downfall.reversky.api.catalog.controller;

import static it.downfall.reversky.api.support.TestUsers.uniqueSuffix;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.dto.FactionRequest;
import it.downfall.reversky.api.catalog.dto.FactionResponse;
import it.downfall.reversky.api.catalog.service.FactionService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link FactionController} via HTTP: l'elenco pubblico delle fazioni, senza token.
 * <p>
 * Le regole su nome ed emblema sono in FactionServiceIT; qui si verifica solo l'accesso pubblico.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class FactionControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FactionService factionService;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa
	// il contesto già avviato (e il suo container) invece di crearne uno nuovo.
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("The list of factions is public: no token needed")
	void findAll_withoutToken_returnsFactions() throws Exception {
		// Arrange
		FactionResponse faction = factionService.create(new FactionRequest("Faction " + uniqueSuffix(), "factions/test.svg", "A test faction"));

		// Act + Assert
		mockMvc.perform(get("/api/v1/factions"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", hasItem(faction.id())))
			.andExpect(jsonPath("$[?(@.id == %d)].name".formatted(faction.id())).value(faction.name()))
			.andExpect(jsonPath("$[?(@.id == %d)].emblemPath".formatted(faction.id())).value("factions/test.svg"))
			.andExpect(jsonPath("$[?(@.id == %d)].description".formatted(faction.id())).value("A test faction"));
	}

	@Test
	@DisplayName("OpenAPI does not ask for a token on GET /factions")
	void apiDocs_factions_hasNoSecurity() throws Exception {
		// Act + Assert
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/factions'].get").exists())
			.andExpect(jsonPath("$.paths['/api/v1/factions'].get.security").doesNotExist());
	}
}
