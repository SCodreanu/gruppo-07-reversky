package it.downfall.reversky.api.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.auth.dto.LoginRequest;
import it.downfall.reversky.api.auth.exception.AccountDisabledException;
import it.downfall.reversky.api.auth.service.AuthService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.inventory.entity.CreditReason;
import it.downfall.reversky.api.inventory.service.InventoryService;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.service.UserService;

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

	@Autowired
	private AuthService authService;

	@Autowired
	private UserService userService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	// --- lista ---

	@Test
	@DisplayName("A USER cannot list the users")
	void findAll_userRole_returns403() throws Exception {
		// Act + Assert
		mockMvc.perform(asUser(get(BASE_PATH)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	@DisplayName("The list finds users by username or email, ignoring case, with role, status and credits")
	void findAll_textSearch_returnsMatchingUsersWithCredits() throws Exception {
		// Arrange: lo username e l'email degli utenti di prova contengono il loro suffisso casuale
		User user = testUsers.create();
		int balance = inventoryService.spendCredits(user.getId(), 10, CreditReason.PACK_PURCHASE);
		String suffix = user.getUsername().substring("user_".length());

		// Act + Assert
		mockMvc.perform(asAdmin(get(BASE_PATH)).param("q", suffix.toUpperCase(Locale.ROOT)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].id").value(user.getId()))
			.andExpect(jsonPath("$.content[0].username").value(user.getUsername()))
			.andExpect(jsonPath("$.content[0].email").value(user.getEmail()))
			.andExpect(jsonPath("$.content[0].role").value("USER"))
			.andExpect(jsonPath("$.content[0].enabled").value(true))
			.andExpect(jsonPath("$.content[0].emailVerified").value(false))
			.andExpect(jsonPath("$.content[0].credits").value(balance))
			.andExpect(jsonPath("$.content[0].createdAt").isNotEmpty());
	}

	@Test
	@DisplayName("The role and enabled filters narrow the list")
	void findAll_roleAndEnabledFilters_narrowList() throws Exception {
		// Arrange: si cerca lo username di ciascuno, così la ricerca lo isola dagli altri test
		User admin = testUsers.createAdmin();
		User disabled = testUsers.create();
		testUsers.disable(disabled);

		// Act + Assert
		mockMvc.perform(asAdmin(get(BASE_PATH)).param("q", admin.getUsername()).param("role", "admin"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id").value(contains(admin.getId())));
		mockMvc.perform(asAdmin(get(BASE_PATH)).param("q", admin.getUsername()).param("role", "USER"))
			.andExpect(jsonPath("$.totalElements").value(0));
		mockMvc.perform(asAdmin(get(BASE_PATH)).param("q", disabled.getUsername()).param("enabled", "false"))
			.andExpect(jsonPath("$.content[*].id").value(contains(disabled.getId())));
		mockMvc.perform(asAdmin(get(BASE_PATH)).param("q", disabled.getUsername()).param("enabled", "true"))
			.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	@DisplayName("An unknown role filter returns an empty page")
	void findAll_unknownRole_returnsEmptyPage() throws Exception {
		// Act + Assert
		mockMvc.perform(asAdmin(get(BASE_PATH)).param("role", "SUPERUSER"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	@DisplayName("The list is paginated and sorted by id")
	void findAll_pagination_sortedById() throws Exception {
		// Arrange: il database è condiviso con gli altri test, quindi si cerca il dominio comune a tutte le email di
		// prova (almeno questi due utenti) e si controllano solo dimensione e ordine della pagina
		testUsers.create();
		testUsers.create();

		// Act + Assert
		String body = mockMvc.perform(asAdmin(get(BASE_PATH)).param("q", "@example.com").param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.size").value(2))
			.andExpect(jsonPath("$.content", hasSize(2)))
			.andReturn().getResponse().getContentAsString();
		List<Integer> ids = JsonPath.read(body, "$.content[*].id");
		assertThat(ids).isSorted();
	}

	// --- disattivazione e riattivazione ---

	@Test
	@DisplayName("Disabling a user blocks the login, deletes the refresh tokens and returns the user")
	void disable_user_blocksLoginAndRevokesSessions() throws Exception {
		// Arrange
		User user = testUsers.create();
		authService.login(new LoginRequest(user.getEmail(), TestUsers.PASSWORD));

		// Act + Assert
		mockMvc.perform(asAdmin(post(BASE_PATH + "/" + user.getId() + "/disable")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(user.getId()))
			.andExpect(jsonPath("$.enabled").value(false));
		assertThat(refreshTokensOf(user)).isZero();
		assertThatThrownBy(() -> authService.login(new LoginRequest(user.getEmail(), TestUsers.PASSWORD)))
			.isInstanceOf(AccountDisabledException.class);
	}

	@Test
	@DisplayName("Disabling a disabled user changes nothing and returns 200")
	void disable_alreadyDisabled_isIdempotent() throws Exception {
		// Arrange
		User user = testUsers.create();
		testUsers.disable(user);

		// Act + Assert
		mockMvc.perform(asAdmin(post(BASE_PATH + "/" + user.getId() + "/disable")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.enabled").value(false));
	}

	@Test
	@DisplayName("An admin cannot disable their own account")
	void disable_self_returns422() throws Exception {
		// Arrange
		User admin = testUsers.createAdmin();

		// Act + Assert
		mockMvc.perform(as(admin, post(BASE_PATH + "/" + admin.getId() + "/disable")))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("ADMIN_SELF_ACTION"));
		assertThat(userService.getById(admin.getId()).isEnabled()).isTrue();
	}

	@Test
	@DisplayName("A USER cannot disable anyone")
	void disable_userRole_returns403() throws Exception {
		// Arrange
		User target = testUsers.create();

		// Act + Assert
		mockMvc.perform(asUser(post(BASE_PATH + "/" + target.getId() + "/disable")))
			.andExpect(status().isForbidden());
		assertThat(userService.getById(target.getId()).isEnabled()).isTrue();
	}

	@Test
	@DisplayName("Enabling a disabled user lets them log in again")
	void enable_disabledUser_allowsLogin() throws Exception {
		// Arrange
		User user = testUsers.create();
		testUsers.disable(user);

		// Act + Assert
		mockMvc.perform(asAdmin(post(BASE_PATH + "/" + user.getId() + "/enable")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.enabled").value(true));
		assertThat(authService.login(new LoginRequest(user.getEmail(), TestUsers.PASSWORD)).accessToken()).isNotBlank();
	}

	@Test
	@DisplayName("Disable, enable and role change on a user that does not exist return 404")
	void actions_unknownUser_return404() throws Exception {
		// Act + Assert
		mockMvc.perform(asAdmin(post(BASE_PATH + "/" + Integer.MAX_VALUE + "/disable")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
		mockMvc.perform(asAdmin(post(BASE_PATH + "/" + Integer.MAX_VALUE + "/enable")))
			.andExpect(status().isNotFound());
		changeRole(testUsers.createAdmin(), Integer.MAX_VALUE, "ADMIN")
			.andExpect(status().isNotFound());
	}

	// --- ruolo ---

	@Test
	@DisplayName("An admin can promote a user to ADMIN and demote them back")
	void changeRole_promoteAndDemote_updatesRole() throws Exception {
		// Arrange
		User admin = testUsers.createAdmin();
		User user = testUsers.create();

		// Act + Assert
		changeRole(admin, user.getId(), "ADMIN")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.role").value("ADMIN"));
		changeRole(admin, user.getId(), "USER")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.role").value("USER"));
	}

	@Test
	@DisplayName("An admin cannot remove the ADMIN role from their own account, but can set it again")
	void changeRole_selfDemotion_returns422() throws Exception {
		// Arrange
		User admin = testUsers.createAdmin();

		// Act + Assert
		changeRole(admin, admin.getId(), "USER")
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("ADMIN_SELF_ACTION"));
		changeRole(admin, admin.getId(), "ADMIN")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	@DisplayName("A role other than USER or ADMIN returns 400")
	void changeRole_invalidRole_returns400() throws Exception {
		// Arrange
		User user = testUsers.create();

		// Act + Assert
		changeRole(testUsers.createAdmin(), user.getId(), "SUPERUSER")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field").value(hasItem("role")));
	}

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

	private ResultActions changeRole(User admin, Integer userId, String role) throws Exception {
		return mockMvc.perform(as(admin, put(BASE_PATH + "/" + userId + "/role"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"role\": \"" + role + "\"}"));
	}

	private Integer refreshTokensOf(User user) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM refresh_tokens WHERE user_id = ?", Integer.class, user.getId());
	}

	private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}

	private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
		return as(testUsers.createAdmin(), request);
	}

	private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(testUsers.create()));
	}
}
