package it.downfall.reversky.api.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.inventory.service.InventoryService;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * {@link AdminUserController} via HTTP, con i filtri di Spring Security attivi.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class AdminUserControllerIT {

	private static final String BASE_PATH = "/api/v1/admin/users";

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

	// --- correzione dei crediti ---

	@Test
	@DisplayName("Without a token a credit correction returns 401")
	void adjustCredits_withoutToken_returns401() throws Exception {
		// Arrange
		User user = testUsers.create();

		// Act + Assert
		mockMvc.perform(post(creditsPath(user.getId())).contentType(MediaType.APPLICATION_JSON).content(creditsBody(10, "Gift")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("A USER cannot correct credits, and the balance does not change")
	void adjustCredits_userRole_returns403AndChangesNothing() throws Exception {
		// Arrange
		User user = testUsers.create();
		int before = inventoryService.getCredits(user.getId());

		// Act + Assert
		mockMvc.perform(asUser(post(creditsPath(user.getId()))).contentType(MediaType.APPLICATION_JSON).content(creditsBody(10, "Gift")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		assertThat(inventoryService.getCredits(user.getId())).isEqualTo(before);
	}

	@Test
	@DisplayName("A positive correction adds credits and returns the new balance")
	void adjustCredits_positive_returnsNewBalance() throws Exception {
		// Arrange
		User user = testUsers.create();
		int before = inventoryService.getCredits(user.getId());

		// Act + Assert
		adjust(user.getId(), creditsBody(75, "Event reward"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.credits").value(before + 75));
		assertThat(inventoryService.getCredits(user.getId())).isEqualTo(before + 75);
	}

	@Test
	@DisplayName("A negative correction removes credits")
	void adjustCredits_negative_removesCredits() throws Exception {
		// Arrange
		User user = testUsers.create();
		int before = inventoryService.getCredits(user.getId());

		// Act + Assert
		adjust(user.getId(), creditsBody(-25, "Duplicate reward"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.credits").value(before - 25));
	}

	@Test
	@DisplayName("A correction below zero returns 422 INSUFFICIENT_CREDITS and changes nothing")
	void adjustCredits_belowZero_returns422() throws Exception {
		// Arrange
		User user = testUsers.create();
		int before = inventoryService.getCredits(user.getId());

		// Act + Assert
		adjust(user.getId(), creditsBody(-(before + 1), "Too much"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("INSUFFICIENT_CREDITS"));
		assertThat(inventoryService.getCredits(user.getId())).isEqualTo(before);
	}

	@Test
	@DisplayName("An amount of zero returns 400")
	void adjustCredits_zeroAmount_returns400() throws Exception {
		// Arrange
		User user = testUsers.create();

		// Act + Assert
		adjust(user.getId(), creditsBody(0, "Nothing"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field").value(hasItem("amountNonZero")));
	}

	@Test
	@DisplayName("A missing amount or a blank reason returns 400")
	void adjustCredits_missingFields_returns400() throws Exception {
		// Arrange
		User user = testUsers.create();

		// Act + Assert
		adjust(user.getId(), "{\"reason\": \"  \"}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field").value(hasItem("amount")))
			.andExpect(jsonPath("$.errors[*].field").value(hasItem("reason")));
	}

	@Test
	@DisplayName("A reason longer than 255 characters returns 400")
	void adjustCredits_reasonTooLong_returns400() throws Exception {
		// Arrange
		User user = testUsers.create();

		// Act + Assert
		adjust(user.getId(), creditsBody(10, "x".repeat(256)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(hasItem("reason")));
	}

	@Test
	@DisplayName("A user that does not exist returns 404")
	void adjustCredits_unknownUser_returns404() throws Exception {
		// Act + Assert
		adjust(Integer.MAX_VALUE, creditsBody(10, "Gift"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("INVENTORY_NOT_FOUND"));
	}

	private ResultActions adjust(Integer userId, String json) throws Exception {
		return mockMvc.perform(asAdmin(post(creditsPath(userId))).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static String creditsPath(Integer userId) {
		return BASE_PATH + "/" + userId + "/credits";
	}

	private static String creditsBody(int amount, String reason) {
		return "{\"amount\": %d, \"reason\": \"%s\"}".formatted(amount, reason);
	}

	private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.createAdmin()));
	}

	private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.create()));
	}
}
