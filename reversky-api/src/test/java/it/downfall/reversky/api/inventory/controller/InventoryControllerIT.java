package it.downfall.reversky.api.inventory.controller;

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

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.inventory.entity.CreditReason;
import it.downfall.reversky.api.inventory.service.InventoryService;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * {@link InventoryController} via HTTP, con i filtri di Spring Security attivi. Le regole del saldo sono in
 * InventoryServiceIT.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerIT {

	private static final String BASE_PATH = "/api/v1/inventory";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private InventoryService inventoryService;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("Without a token the inventory returns 401")
	void getInventory_withoutToken_returns401() throws Exception {
		// Act + Assert
		mockMvc.perform(get(BASE_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("The inventory shows the balance of the token's user")
	void getInventory_validToken_returnsOwnBalance() throws Exception {
		// Arrange: un altro utente con un saldo diverso non deve comparire
		User user = testUsers.create();
		User other = testUsers.create();
		int balance = inventoryService.spendCredits(user.getId(), 120, CreditReason.PACK_PURCHASE);
		inventoryService.addCredits(other.getId(), 999, CreditReason.BATTLE_WIN);

		// Act + Assert
		mockMvc.perform(get(BASE_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.credits").value(balance));
	}

	@Test
	@DisplayName("OpenAPI requires the Bearer token on /inventory")
	void apiDocs_inventory_requiresBearerToken() throws Exception {
		// Act + Assert
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/inventory'].get.security[0]").value(hasKey("bearerAuth")));
	}
}
