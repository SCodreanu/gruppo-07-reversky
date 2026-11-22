package it.downfall.reversky.api.battle.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link MatchmakingTicket#searchWindow}: {@code min(100 + 50 × (secondi di attesa / 10, per difetto), 400)}
 * (docs/sfide, "Abbinamento").
 */
class MatchmakingTicketTest {

	private static final Instant QUEUED = Instant.parse("2026-11-22T10:00:00Z");

	private final MatchmakingTicket ticket = new MatchmakingTicket(7, 3, 1000, QUEUED);

	@Test
	@DisplayName("The window starts at 100 and grows by 50 every full 10 seconds: 9 s is still 100, 10 s is 150")
	void searchWindow_growsEveryTenSeconds() {
		assertThat(ticket.searchWindow(QUEUED)).isEqualTo(100);
		assertThat(ticket.searchWindow(QUEUED.plusSeconds(9))).isEqualTo(100);
		assertThat(ticket.searchWindow(QUEUED.plusSeconds(10))).isEqualTo(150);
	}

	@Test
	@DisplayName("After 35 seconds the window is 250, as in the docs example")
	void searchWindow_docsExample() {
		assertThat(ticket.searchWindow(QUEUED.plusSeconds(35))).isEqualTo(250);
	}

	@Test
	@DisplayName("The window stops at 400: reached at 60 seconds, still 400 after 10 minutes")
	void searchWindow_cappedAt400() {
		assertThat(ticket.searchWindow(QUEUED.plusSeconds(59))).isEqualTo(350);
		assertThat(ticket.searchWindow(QUEUED.plusSeconds(60))).isEqualTo(400);
		assertThat(ticket.searchWindow(QUEUED.plusSeconds(600))).isEqualTo(400);
	}

	@Test
	@DisplayName("A clock behind the ticket time gives the initial window, never less")
	void searchWindow_clockBehind_initialWindow() {
		assertThat(ticket.searchWindow(QUEUED.minusSeconds(30))).isEqualTo(100);
	}
}
