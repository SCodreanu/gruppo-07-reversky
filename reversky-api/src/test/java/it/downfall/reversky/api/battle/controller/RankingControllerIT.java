package it.downfall.reversky.api.battle.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.battle.dto.BattleOutcome;
import it.downfall.reversky.api.battle.entity.PlayerRating;
import it.downfall.reversky.api.battle.repository.PlayerRatingRepository;
import it.downfall.reversky.api.battle.service.RankingService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.common.security.JwtService;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * {@link RankingController} e {@link RankingService} sul database vero (API-46): il mio rating, la riga creata al
 * primo ingresso in coda e i controlli della tabella {@code player_ratings}.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
@AutoConfigureMockMvc
class RankingControllerIT {

	private static final String PATH = "/api/v1/rankings/me";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private RankingService rankingService;

	@Autowired
	private PlayerRatingRepository playerRatingRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// Non serve al test: con la stessa configurazione degli altri ControllerIT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("Without a token my rating returns 401")
	void getMine_withoutToken_returns401() throws Exception {
		mockMvc.perform(get(PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("Before the first ranked queue I get 1000 and zero games, and no row is created")
	void getMine_noRow_initialValuesWithoutWriting() throws Exception {
		// Arrange
		User user = testUsers.create();

		// Act + Assert
		mockMvc.perform(as(user, get(PATH)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rating").value(1000))
			.andExpect(jsonPath("$.peakRating").value(1000))
			.andExpect(jsonPath("$.gamesPlayed").value(0))
			.andExpect(jsonPath("$.wins").value(0))
			.andExpect(jsonPath("$.losses").value(0))
			.andExpect(jsonPath("$.draws").value(0));
		assertThat(rows(user)).isZero();
	}

	@Test
	@DisplayName("After some ranked games I see my rating, peak and counters, not someone else's")
	void getMine_withGames_returnsOwnRow() throws Exception {
		// Arrange: due partite salvate sul database, che ne controlla i vincoli; un altro utente con un altro rating
		User user = testUsers.create();
		User other = testUsers.create();
		save(user, BattleOutcome.WIN, 13, BattleOutcome.LOSS, -20);
		save(other, BattleOutcome.WIN, 40, BattleOutcome.WIN, 30);

		// Act + Assert
		mockMvc.perform(as(user, get(PATH)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rating").value(993))
			.andExpect(jsonPath("$.peakRating").value(1013))
			.andExpect(jsonPath("$.gamesPlayed").value(2))
			.andExpect(jsonPath("$.wins").value(1))
			.andExpect(jsonPath("$.losses").value(1))
			.andExpect(jsonPath("$.draws").value(0));
	}

	@Test
	@DisplayName("createIfMissing creates the row with the column defaults once; the second call does nothing")
	void createIfMissing_createsOnce() {
		// Arrange
		User user = testUsers.create();

		// Act
		boolean first = rankingService.createIfMissing(user.getId());
		boolean second = rankingService.createIfMissing(user.getId());

		// Assert
		assertThat(first).isTrue();
		assertThat(second).isFalse();
		assertThat(rows(user)).isEqualTo(1);
		PlayerRating rating = playerRatingRepository.findById(user.getId()).orElseThrow();
		assertThat(rating.getRating()).isEqualTo(PlayerRating.INITIAL_RATING);
		assertThat(rating.getPeakRating()).isEqualTo(PlayerRating.INITIAL_RATING);
		assertThat(rating.getGamesPlayed()).isZero();
	}

	// Crea la riga e applica due partite, come farà la partita classificata
	private void save(User user, BattleOutcome firstOutcome, int firstDelta, BattleOutcome secondOutcome,
			int secondDelta) {
		rankingService.createIfMissing(user.getId());
		PlayerRating rating = playerRatingRepository.findById(user.getId()).orElseThrow();
		rating.recordGame(firstOutcome, firstDelta);
		rating.recordGame(secondOutcome, secondDelta);
		playerRatingRepository.save(rating);
	}

	private int rows(User user) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM player_ratings WHERE user_id = ?", Integer.class,
			user.getId());
	}

	private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(user));
	}
}
