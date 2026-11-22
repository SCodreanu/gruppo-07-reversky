package it.downfall.reversky.api.catalog.controller;

import static it.downfall.reversky.api.support.TestUsers.uniqueSuffix;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.dto.RarityCreateRequest;
import it.downfall.reversky.api.catalog.dto.RarityResponse;
import it.downfall.reversky.api.catalog.service.RarityService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link RarityController} via HTTP: l'elenco pubblico delle rarità, senza token.
 * <p>
 * Le regole e l'ordinamento sono in RarityServiceIT; qui si verificano l'accesso pubblico e il JSON.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class RarityControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RarityService rarityService;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa
	// il contesto già avviato (e il suo container) invece di crearne uno nuovo.
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("The list of rarities is public: no token needed")
	void findAll_withoutToken_returnsRarities() throws Exception {
		// Arrange
		int sortingOrder = ThreadLocalRandom.current().nextInt(1_000, 1_000_000_000);
		RarityResponse rarity = rarityService.create(new RarityCreateRequest("Rarity " + uniqueSuffix(), 2, null, sortingOrder));
		String byId = "$[?(@.id == %d)]".formatted(rarity.id());

		// Act + Assert: maxPerDeck vuoto arriva come null, non sparisce dal JSON
		mockMvc.perform(get("/api/v1/rarities"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", hasItem(rarity.id())))
			.andExpect(jsonPath(byId + ".name").value(rarity.name()))
			.andExpect(jsonPath(byId + ".deckLimit").value(2))
			.andExpect(jsonPath(byId).value(hasItem(hasEntry(equalTo("maxPerDeck"), nullValue()))))
			.andExpect(jsonPath(byId + ".sortingOrder").value(sortingOrder));
	}

	@Test
	@DisplayName("OpenAPI does not ask for a token on GET /rarities")
	void apiDocs_rarities_hasNoSecurity() throws Exception {
		// Act + Assert
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/rarities'].get").exists())
			.andExpect(jsonPath("$.paths['/api/v1/rarities'].get.security").doesNotExist());
	}
}
